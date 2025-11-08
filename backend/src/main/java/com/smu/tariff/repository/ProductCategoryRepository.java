package com.smu.tariff.repository;

import com.smu.tariff.model.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
    Optional<ProductCategory> findByCode(String code);
    Optional<ProductCategory> findByNameIgnoreCase(String name);
}
