package com.smu.tariff.model;

import jakarta.persistence.*;

@Entity
@Table(name = "product_category")
public class ProductCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "hs_code")
    private String hsCode;

    @Column(name = "weight_based", nullable = false)
    private boolean weightBased = false;

    public ProductCategory() { }

    public ProductCategory(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public ProductCategory(String code, String name, String hsCode, Boolean weightBased) {
        this.code = code;
        this.name = name;
        this.hsCode = hsCode;
        this.weightBased = weightBased == null ? false : weightBased.booleanValue();
    }

    public Long getId() { return id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHsCode() { return hsCode; }
    public void setHsCode(String hsCode) { this.hsCode = hsCode; }

    public boolean getWeightBased() { return weightBased; }
    public void setWeightBased(Boolean weightBased) { this.weightBased = weightBased == null ? false : weightBased.booleanValue(); }
}
