package com.smu.tariff.trade;

import com.smu.tariff.country.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface TradeFlowRepository extends JpaRepository<TradeFlow, Long> {

    interface ProductSummary {
        String getCode();
        String getName();
        BigDecimal getTotalValue();
    }

    interface PartnerSummary {
        String getCode();
        String getName();
        BigDecimal getTotalValue();
    }

    @Query("SELECT tf.productCategory.code AS code, tf.productCategory.name AS name, SUM(tf.tradeValue) AS totalValue " +
           "FROM TradeFlow tf " +
           "WHERE tf.reportingCountry = :country AND tf.direction = :direction " +
           "GROUP BY tf.productCategory.code, tf.productCategory.name " +
           "ORDER BY SUM(tf.tradeValue) DESC")
    List<ProductSummary> findTopProductsByCountryAndDirection(@Param("country") Country country,
                                                              @Param("direction") TradeDirection direction);

    @Query("SELECT tf.partnerCountry.code AS code, tf.partnerCountry.name AS name, SUM(tf.tradeValue) AS totalValue " +
           "FROM TradeFlow tf " +
           "WHERE tf.reportingCountry = :country " +
           "GROUP BY tf.partnerCountry.code, tf.partnerCountry.name " +
           "ORDER BY SUM(tf.tradeValue) DESC")
    List<PartnerSummary> findPartnersByCountry(@Param("country") Country country);
}
