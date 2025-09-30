package com.smu.tariff.tariff;

import com.smu.tariff.country.Country;
import com.smu.tariff.product.ProductCategory;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TariffRateRepository extends JpaRepository<TariffRate, Long> {

    @Query("SELECT t FROM TariffRate t WHERE t.origin = :origin AND t.destination = :destination " +
           "AND t.productCategory = :category AND t.effectiveFrom <= :date " +
           "AND (t.effectiveTo IS NULL OR t.effectiveTo >= :date) ORDER BY t.effectiveFrom DESC")
    List<TariffRate> findApplicableRates(@Param("origin") Country origin,
                                         @Param("destination") Country destination,
                                         @Param("category") ProductCategory category,
                                         @Param("date") LocalDate date);

    @Query("SELECT t FROM TariffRate t WHERE (:origin IS NULL OR t.origin = :origin) " +
            "AND (:destination IS NULL OR t.destination = :destination) " +
            "AND (:category IS NULL OR t.productCategory = :category)")
    List<TariffRate> search(@Param("origin") Country origin,
                            @Param("destination") Country destination,
                            @Param("category") ProductCategory category);

    Optional<TariffRate> findTop1ByOriginAndDestinationAndProductCategoryOrderByEffectiveFromDesc(
            Country origin, Country destination, ProductCategory category);

    // Fallback: find the latest rate for a product category where baseRate > 0
    Optional<TariffRate> findFirstByProductCategoryAndBaseRateGreaterThanOrderByEffectiveFromDesc(
            ProductCategory category, java.math.BigDecimal threshold);
}
