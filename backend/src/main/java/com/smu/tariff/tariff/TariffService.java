package com.smu.tariff.tariff;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smu.tariff.country.Country;
import com.smu.tariff.country.CountryRepository;
import com.smu.tariff.exception.InvalidTariffRequestException;
import com.smu.tariff.exception.TariffNotFoundException;
import com.smu.tariff.logging.QueryLog;
import com.smu.tariff.logging.QueryLogRepository;
import com.smu.tariff.product.ProductCategory;
import com.smu.tariff.product.ProductCategoryRepository;
import com.smu.tariff.tariff.dto.TariffCalcRequest;
import com.smu.tariff.tariff.dto.TariffCalcResponse;
import com.smu.tariff.tariff.dto.TariffRateDto;
import com.smu.tariff.user.User;

@Service
@Transactional
public class TariffService {

    private final TariffRateRepository tariffRateRepository;
    private final CountryRepository countryRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final QueryLogRepository queryLogRepository;

    public TariffService(TariffRateRepository tariffRateRepository,
            CountryRepository countryRepository,
            ProductCategoryRepository productCategoryRepository,
            QueryLogRepository queryLogRepository) {
        this.tariffRateRepository = tariffRateRepository;
        this.countryRepository = countryRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.queryLogRepository = queryLogRepository;
    }

    public TariffCalcResponse calculate(TariffCalcRequest req) {
        // Validate input
        if (req.originCountryCode == null || req.originCountryCode.trim().isEmpty()) {
            throw new InvalidTariffRequestException("Origin country code is required");
        }
        if (req.destinationCountryCode == null || req.destinationCountryCode.trim().isEmpty()) {
            throw new InvalidTariffRequestException("Destination country code is required");
        }
        if (req.productCategoryCode == null || req.productCategoryCode.trim().isEmpty()) {
            throw new InvalidTariffRequestException("Product category code is required");
        }
        if (req.declaredValue <= 0) {
            throw new InvalidTariffRequestException("Declared value must be greater than 0");
        }

        LocalDate date = (req.date == null || req.date.isBlank()) ? LocalDate.now() : LocalDate.parse(req.date);

        Country origin = countryRepository.findByCode(req.originCountryCode.toUpperCase())
                .orElseThrow(() -> new InvalidTariffRequestException(
                        "Unknown origin country code: " + req.originCountryCode));
        Country dest = countryRepository.findByCode(req.destinationCountryCode.toUpperCase())
                .orElseThrow(() -> new InvalidTariffRequestException(
                        "Unknown destination country code: " + req.destinationCountryCode));
        ProductCategory cat = productCategoryRepository.findByCode(req.productCategoryCode.toUpperCase())
                .orElseThrow(() -> new InvalidTariffRequestException(
                        "Unknown product category code: " + req.productCategoryCode));

        List<TariffRate> rates = tariffRateRepository.findApplicableRates(origin, dest, cat, date);
        if (rates.isEmpty()) {
            throw new TariffNotFoundException(
                    "No applicable tariff rate found for route %s -> %s, category %s on %s".formatted(
                            req.originCountryCode, req.destinationCountryCode, req.productCategoryCode, date));
        }
        TariffRate rate = rates.get(0); // latest effective rate

        BigDecimal declared = BigDecimal.valueOf(req.declaredValue).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tariffAmount = declared.multiply(rate.getBaseRate()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = declared.add(tariffAmount).add(rate.getAdditionalFee()).setScale(2, RoundingMode.HALF_UP);

        TariffCalcResponse resp = new TariffCalcResponse();
        resp.originCountryCode = origin.getCode();
        resp.destinationCountryCode = dest.getCode();
        resp.productCategoryCode = cat.getCode();
        resp.effectiveDate = date.toString();
        resp.declaredValue = declared;
        resp.baseRate = rate.getBaseRate();
        resp.tariffAmount = tariffAmount;
        resp.additionalFee = rate.getAdditionalFee();
        resp.totalCost = total;
        resp.notes = "Total = declaredValue + (declaredValue * baseRate) + additionalFee";

        logQuery("CALCULATE", "{origin:%s,dest:%s,cat:%s,val:%s,date:%s}".formatted(
                resp.originCountryCode, resp.destinationCountryCode, resp.productCategoryCode, declared, date));

        return resp;
    }

    public List<TariffRateDto> search(String originCode, String destCode, String catCode) {
        Country origin = null;
        Country dest = null;
        ProductCategory cat = null;

        if (originCode != null && !originCode.trim().isEmpty()) {
            origin = countryRepository.findByCode(originCode)
                    .orElseThrow(() -> new InvalidTariffRequestException("Unknown origin country code: " + originCode));
        }

        if (destCode != null && !destCode.trim().isEmpty()) {
            dest = countryRepository.findByCode(destCode)
                    .orElseThrow(
                            () -> new InvalidTariffRequestException("Unknown destination country code: " + destCode));
        }

        if (catCode != null && !catCode.trim().isEmpty()) {
            cat = productCategoryRepository.findByCode(catCode.toUpperCase())
                    .orElseThrow(() -> new InvalidTariffRequestException("Unknown product category code: " + catCode));
        }

        List<TariffRate> list = tariffRateRepository.search(origin, dest, cat);

        List<TariffRateDto> dtos = list.stream().map(t -> {
            TariffRateDto dto = new TariffRateDto();
            dto.id = t.getId();
            dto.originCountryCode = t.getOrigin().getCode();
            dto.destinationCountryCode = t.getDestination().getCode();
            dto.productCategoryCode = t.getProductCategory().getCode();
            dto.baseRate = t.getBaseRate();
            dto.additionalFee = t.getAdditionalFee();
            dto.effectiveFrom = t.getEffectiveFrom();
            dto.effectiveTo = t.getEffectiveTo();
            return dto;
        }).collect(java.util.stream.Collectors.toList());

        logQuery("SEARCH", "{origin:%s,dest:%s,cat:%s}".formatted(originCode, destCode, catCode));
        return dtos;
    }

    private void logQuery(String type, String params) {
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (auth != null && auth.getPrincipal() instanceof User) ? (User) auth.getPrincipal() : null;
        queryLogRepository.save(new QueryLog(user, type, params));
    }

    // CREATE a new tariff rule
    public TariffRateDtoPost createTariff(TariffRateDtoPost dto) {
    Country origin = countryRepository.findByCode(dto.originCountryCode)
    .orElseThrow(() -> new InvalidTariffRequestException("Unknown origin country code: " + dto.originCountryCode));

    Country dest = countryRepository.findByCode(dto.destinationCountryCode)
    .orElseThrow(() -> new InvalidTariffRequestException("Unknown destination country code: " + dto.destinationCountryCode));

    ProductCategory cat = productCategoryRepository.findByCode(dto.productCategoryCode)
        .orElseThrow(() -> new RuntimeException("Category not found: " + dto.productCategoryCode));


    TariffRate tariff = new TariffRate(origin, dest, cat, dto.baseRate, dto.additionalFee,
                                       dto.effectiveFrom, dto.effectiveTo);
    TariffRate saved = tariffRateRepository.save(tariff);

    return mapToDto(saved);
}

    // Utility method to map entity -> DTO
    private TariffRateDto mapToDto(TariffRate t) {
        TariffRateDto dto = new TariffRateDto();
        dto.id = t.getId();
        dto.originCountryCode = t.getOrigin().getCode();
        dto.destinationCountryCode = t.getDestination().getCode();
        dto.productCategoryCode = t.getProductCategory().getCode();
        dto.baseRate = t.getBaseRate();
        dto.additionalFee = t.getAdditionalFee();
        dto.effectiveFrom = t.getEffectiveFrom();
        dto.effectiveTo = t.getEffectiveTo();
        return dto;
    }

    // READ ALL
    public List<TariffRateDto> getAllTariffs() {
        List<TariffRate> list = tariffRateRepository.findAll();
        return list.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    // READ ONE BY ID
    public TariffRateDto getTariffById(Long id) {
        TariffRate rate = tariffRateRepository.findById(id)
                .orElseThrow(() -> new TariffNotFoundException("Tariff with id " + id + " not found"));
        return mapToDto(rate);
    }

    // UPDATE
    public TariffRateDto updateTariff(Long id, TariffRateDto dto) {
        TariffRate rate = tariffRateRepository.findById(id)
                .orElseThrow(() -> new TariffNotFoundException("Tariff with id " + id + " not found"));

        if (dto.originCountryCode != null) {
            Country origin = countryRepository.findByCode(dto.originCountryCode.toUpperCase())
                    .orElseThrow(() -> new InvalidTariffRequestException("Unknown origin country code: " + dto.originCountryCode));
            rate.setOrigin(origin);
        }
        if (dto.destinationCountryCode != null) {
            Country dest = countryRepository.findByCode(dto.destinationCountryCode.toUpperCase())
                    .orElseThrow(() -> new InvalidTariffRequestException("Unknown destination country code: " + dto.destinationCountryCode));
            rate.setDestination(dest);
        }
        if (dto.productCategoryCode != null) {
            ProductCategory cat = productCategoryRepository.findByCode(dto.productCategoryCode.toUpperCase())
                    .orElseThrow(() -> new InvalidTariffRequestException("Unknown product category code: " + dto.productCategoryCode));
            rate.setProductCategory(cat);
        }
        if (dto.baseRate != null) rate.setBaseRate(dto.baseRate);
        if (dto.additionalFee != null) rate.setAdditionalFee(dto.additionalFee);
        if (dto.effectiveFrom != null) rate.setEffectiveFrom(dto.effectiveFrom);
        if (dto.effectiveTo != null) rate.setEffectiveTo(dto.effectiveTo);

        tariffRateRepository.save(rate);

        logQuery("UPDATE", "id=" + id + ", " + dto.toString());

        return mapToDto(rate);
    }

    // DELETE
    public void deleteTariff(Long id) {
        if (!tariffRateRepository.existsById(id)) {
            throw new TariffNotFoundException("Tariff with id " + id + " not found");
        }
        tariffRateRepository.deleteById(id);
        logQuery("DELETE", "id=" + id);
    }

    // ^CRUD satisfied

}
