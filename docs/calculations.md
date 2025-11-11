# Trade Analytics Calculation Methods

This document explains all the calculation methods used in the Trade Analytics Service for the CS203 Tariff Platform.

## Overview

The Trade Analytics Service processes real tariff data from the PostgreSQL database and generates insights by calculating averages, grouping by categories, and formatting for display.

## Data Sources

All calculations are based on the `tariff_rate` table with the following structure:
- `base_rate`: Percentage value stored between 0 and 100 (e.g., 2.2000 for 2.2%). Services divide by 100 to convert it into a decimal before performing any calculations.
- `additional_fee`: Currency amount (e.g., 16.00 for $16.00)
- `origin_id`: Country where goods originate
- `destination_id`: Country where goods are imported to
- `product_category_id`: Product category classification
- `effective_from` / `effective_to`: Date validity range

## Main Calculation Methods

### 1. Average Tariff Calculation

**Method**: `computeAverageTariff(List<TariffRate> rates)`

**Purpose**: Calculates the overall average base rate for a list of tariff rates.

**Formula**:
```
Average Base Rate = Σ(base_rate) / count(rates)
```

**Implementation**:
```java
List<BigDecimal> baseRates = rates.stream()
    .map(TariffRate::getBaseRate)
    .filter(Objects::nonNull)
    .collect(Collectors.toList());
if (baseRates.isEmpty()) {
    return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
}
BigDecimal sum = baseRates.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
return sum.divide(BigDecimal.valueOf(baseRates.size()), 4, RoundingMode.HALF_UP);
```

Null `base_rate` entries are ignored, and if no valid rates remain the method returns `0.0000`.

**Output**: Decimal with 4 decimal places (e.g., 0.0236 for 2.36%)

**Example**:
- Input rates: [0.0220, 0.0270, 0.0200]
- Sum: 0.0690
- Count: 3
- Result: 0.0230 (2.30%)

---

### 2. Product Category Metrics Calculation

**Method**: `generateTopProducts(List<TariffRate> tariffRates)`

**Purpose**: Groups tariff rates by product category and calculates average base rate and additional fee for each category.

#### 2.1 Base Rate Calculation per Product

**Formula**:
```
Average Base Rate % = (Σ(base_rate) / count(valid_rates)) × 100
```

**Implementation**:
```java
List<BigDecimal> baseRates = rates.stream()
    .map(TariffRate::getBaseRate)
    .filter(Objects::nonNull)  // Handle null values
    .collect(Collectors.toList());

if (!baseRates.isEmpty()) {
    dto.baseRate = baseRates.stream()
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .divide(BigDecimal.valueOf(baseRates.size()), 4, RoundingMode.HALF_UP)
        .multiply(BigDecimal.valueOf(100)); // Convert to percentage
}
```

> The application converts the stored percentage to a decimal (divide by 100) before executing the formula above.

**Example for Electronics (ELEC)**:
- Database records: [0.0220, 0.0270] (base rates for Electronics)
- Sum: 0.0490
- Average: 0.0245
- Percentage: 2.45%

#### 2.2 Additional Fee Calculation per Product

**Formula**:
```
Average Additional Fee = Σ(additional_fee) / count(valid_fees)
```

**Implementation**:
```java
List<BigDecimal> additionalFees = rates.stream()
    .map(TariffRate::getAdditionalFee)
    .filter(Objects::nonNull)  // Handle null values
    .collect(Collectors.toList());

if (!additionalFees.isEmpty()) {
    dto.additionalFee = additionalFees.stream()
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .divide(BigDecimal.valueOf(additionalFees.size()), 2, RoundingMode.HALF_UP);
}
```

**Example for Electronics (ELEC)**:
- Database records: [16.00, 19.00] (additional fees for Electronics)
- Sum: 35.00
- Average: $17.50

Categories that have no valid base rates *and* no valid additional fees after filtering are skipped so analytics never surface empty placeholders. The `totalValue` used for sorting equals the computed base-rate percentage (or zero when unavailable) to keep ordering deterministic.

---

### 3. Trading Partners Calculation

**Method**: `generateTradingPartners(List<TariffRate> importTariffs, List<TariffRate> exportTariffs)`

**Purpose**: Identifies major trading partners and calculates average tariff rates for each partner relationship.

#### 3.1 Import Partners Processing

**Formula**:
```
Import Tariff Rate % = (sum(base_rate_from_partner) / count(valid_rates)) x 100
Result is capped at 100%.
```

**Implementation**:
```java
Map<String, List<TariffRate>> importPartners = importTariffs.stream()
    .collect(Collectors.groupingBy(t -> t.getOrigin().getCode()));

for (Map.Entry<String, List<TariffRate>> entry : importPartners.entrySet()) {
    List<TariffRate> rates = entry.getValue();
    BigDecimal sum = rates.stream()
        .map(TariffRate::getBaseRate)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal avgBaseRate = sum.divide(BigDecimal.valueOf(rates.size()), 6, RoundingMode.HALF_UP);
    BigDecimal avgBaseRatePercent = capPercentage(avgBaseRate.multiply(BigDecimal.valueOf(100)));

    PartnerMetricDto dto = new PartnerMetricDto();
    dto.totalValue = avgBaseRatePercent;
    dto.rateCount = rates.size();
    partnerMap.put(entry.getKey(), dto);
}
```

#### 3.2 Export Partners Processing

Same as imports, but grouped by destination country (`t -> t.getDestination().getCode()`).

#### 3.3 Combined Partner Metrics

**Formula**:
```
Combined Rate % = ( (Import Rate % x import_count) + (Export Rate % x export_count) )
                  / (import_count + export_count)
```

**Implementation**:
```java
if (existing != null) {
    BigDecimal combinedSum = existing.totalValue
        .multiply(BigDecimal.valueOf(existing.rateCount))
        .add(avgBaseRatePercent.multiply(BigDecimal.valueOf(rates.size())));
    int totalRates = existing.rateCount + rates.size();
    existing.totalValue = combinedSum.divide(BigDecimal.valueOf(totalRates), 4, RoundingMode.HALF_UP);
    existing.rateCount = totalRates;
} else {
    PartnerMetricDto dto = new PartnerMetricDto();
    dto.code = code;
    dto.name = rates.get(0).getDestination().getName();
    dto.totalValue = avgBaseRatePercent;
    dto.rateCount = rates.size();
    partnerMap.put(code, dto);
}
```

Every intermediate percentage passes through the same `capPercentage` helper so the UI never shows values above 100%.
---

## Data Processing Pipeline

### 1. Data Retrieval

```sql
-- Import tariffs (TO Singapore)
SELECT * FROM tariff_rate WHERE destination_id = ? AND origin_id IS NULL AND product_category_id IS NULL

-- Export tariffs (FROM Singapore)  
SELECT * FROM tariff_rate WHERE origin_id = ? AND destination_id IS NULL AND product_category_id IS NULL
```

### 2. Data Grouping

- **Products**: Group by `product_category.code`
- **Partners**: Group by `origin.code` (imports) and `destination.code` (exports)

### 3. Null Handling

All calculations include null filtering:
```java
.filter(Objects::nonNull)  // Remove null values before calculation
```

### 4. Precision and Rounding

- **Base rates**: 4 decimal places, HALF_UP rounding
- **Additional fees**: 2 decimal places, HALF_UP rounding
- **Percentages**: Converted by multiplying by 100

---

## Actual Database Example

Based on the tariff_rate table for Singapore (SGP):

### Import Records (destination_id = 3)
| ID | Origin | Base Rate | Additional Fee | Product |
|----|--------|-----------|----------------|---------|
| 12 | CHN    | 0.0220    | 16.00         | ELEC    |
| 16 | ???    | 0.0270    | 19.00         | ELEC    |
| 4  | ???    | 0.0300    | 20.00         | ???     |
| 6  | ???    | 0.0550    | 35.00         | ???     |
| 18 | ???    | 0.0450    | 32.00         | ???     |

### Calculated Results for Electronics (ELEC)
- **Base Rate**: (0.0220 + 0.0270) / 2 × 100 = **2.45%**
- **Additional Fee**: (16.00 + 19.00) / 2 = **$17.50**

---

## Frontend Display

The calculated values are sent to the frontend as:
```json
{
  "topImports": [
    {
      "code": "ELEC",
      "name": "Electronics", 
      "baseRate": 2.45,
      "additionalFee": 17.50,
      "totalValue": 2.45
    }
  ]
}
```

Frontend formatting:
- `formatPercent(2.45)` → "2.45%"
- `formatCurrency(17.50)` → "$17.50"

---

## Constants and Limits

- `MAX_ITEMS = 5`: Maximum number of items returned for each category
- `RoundingMode.HALF_UP`: Standard rounding method
- `100%` cap on partner tariff rates to prevent unrealistic percentages
- Scale 4 for base rates, scale 2 for currency amounts

---

## Error Handling

1. **Null Values**: Filtered out before calculations
2. **Empty Lists**: Return null or zero values with appropriate logging
3. **Division by Zero**: Prevented by checking list size before division
4. **Invalid Countries**: Throws `InvalidTariffRequestException`

---

## Performance Considerations

- Uses Java 8 Streams for efficient data processing
- Groups data in memory rather than multiple database queries
- Lazy loading for country and product category entities
- Transaction boundary: `@Transactional(readOnly = true)`



