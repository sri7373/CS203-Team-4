package com.smu.tariff.tariff;

import com.smu.tariff.country.Country;
import com.smu.tariff.product.ProductCategory;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tariff_rate",
       uniqueConstraints = @UniqueConstraint(columnNames = {"origin_id", "destination_id", "product_category_id", "effective_from"}))
public class TariffRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_id")
    private Country origin;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id")
    private Country destination;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_category_id")
    private ProductCategory productCategory;

    @Column(nullable = false, precision = 8, scale = 4)
    private BigDecimal baseRate; // e.g., 0.05 for 5%

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal additionalFee; // flat fee in destination currency

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    @Column
    private LocalDate effectiveTo; // optional; null means still effective

    public TariffRate() { }

    public TariffRate(Country origin, Country destination, ProductCategory productCategory,
                      BigDecimal baseRate, BigDecimal additionalFee,
                      LocalDate effectiveFrom, LocalDate effectiveTo) {
        this.origin = origin;
        this.destination = destination;
        this.productCategory = productCategory;
        this.baseRate = baseRate;
        this.additionalFee = additionalFee;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
    }

    public Long getId() { return id; }
    public Country getOrigin() { return origin; }
    public Country getDestination() { return destination; }
    public ProductCategory getProductCategory() { return productCategory; }
    public BigDecimal getBaseRate() { return baseRate; }
    public BigDecimal getAdditionalFee() { return additionalFee; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }

    public void setOrigin(Country origin) { this.origin = origin; }
    public void setDestination(Country destination) { this.destination = destination; }
    public void setProductCategory(ProductCategory productCategory) { this.productCategory = productCategory; }
    public void setBaseRate(BigDecimal baseRate) { this.baseRate = baseRate; }
    public void setAdditionalFee(BigDecimal additionalFee) { this.additionalFee = additionalFee; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
}
