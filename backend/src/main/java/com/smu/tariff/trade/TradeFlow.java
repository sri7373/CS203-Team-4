package com.smu.tariff.trade;

import com.smu.tariff.country.Country;
import com.smu.tariff.product.ProductCategory;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "trade_flow",
       indexes = {
           @Index(name = "idx_trade_flow_reporting_direction", columnList = "reporting_country_id,direction"),
           @Index(name = "idx_trade_flow_partner", columnList = "partner_country_id"),
           @Index(name = "idx_trade_flow_product", columnList = "product_category_id")
       })
public class TradeFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reporting_country_id")
    private Country reportingCountry;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_country_id")
    private Country partnerCountry;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_category_id")
    private ProductCategory productCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TradeDirection direction;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal tradeValue;

    @Column(nullable = false)
    private int year;

    public TradeFlow() { }

    public TradeFlow(Country reportingCountry,
                     Country partnerCountry,
                     ProductCategory productCategory,
                     TradeDirection direction,
                     BigDecimal tradeValue,
                     int year) {
        this.reportingCountry = reportingCountry;
        this.partnerCountry = partnerCountry;
        this.productCategory = productCategory;
        this.direction = direction;
        this.tradeValue = tradeValue;
        this.year = year;
    }

    public Long getId() {
        return id;
    }

    public Country getReportingCountry() {
        return reportingCountry;
    }

    public void setReportingCountry(Country reportingCountry) {
        this.reportingCountry = reportingCountry;
    }

    public Country getPartnerCountry() {
        return partnerCountry;
    }

    public void setPartnerCountry(Country partnerCountry) {
        this.partnerCountry = partnerCountry;
    }

    public ProductCategory getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(ProductCategory productCategory) {
        this.productCategory = productCategory;
    }

    public TradeDirection getDirection() {
        return direction;
    }

    public void setDirection(TradeDirection direction) {
        this.direction = direction;
    }

    public BigDecimal getTradeValue() {
        return tradeValue;
    }

    public void setTradeValue(BigDecimal tradeValue) {
        this.tradeValue = tradeValue;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }
}
