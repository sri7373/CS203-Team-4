package com.smu.tariff.tariff;

import com.smu.tariff.exception.InvalidTariffRequestException;
import com.smu.tariff.model.ProductCategory;
import com.smu.tariff.repository.ProductCategoryRepository;
import com.smu.tariff.tariff.dto.CalculateTariffRequestDto;
import com.smu.tariff.tariff.dto.CalculateTariffResponseDto;
import com.smu.tariff.tariff.dto.TariffCalcRequest;
import com.smu.tariff.tariff.dto.TariffCalcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api")
@Validated
public class CalculateTariffController {

    private static final Logger log = LoggerFactory.getLogger(CalculateTariffController.class);

    private final TariffService tariffService;
    private final ProductCategoryRepository productCategoryRepository;

    public CalculateTariffController(TariffService tariffService, ProductCategoryRepository productCategoryRepository) {
        this.tariffService = tariffService;
        this.productCategoryRepository = productCategoryRepository;
    }

    /**
     * Calculate tariff endpoint.
     * Request must include declaredValue. If product is weight-based, declaredValue will be multiplied by weight.
     */
    @PostMapping("/calculate-tariff")
    @Transactional(readOnly = true)
    public CalculateTariffResponseDto calculateTariff(@Valid @RequestBody CalculateTariffRequestDto req) {
        log.info("Received calculate-tariff request for product {}", req.getProductCode());

        if (req.getDeclaredValue() == null || req.getDeclaredValue() <= 0) {
            throw new InvalidTariffRequestException("declaredValue must be provided and greater than 0");
        }

        // find product category
        ProductCategory cat = productCategoryRepository.findByCode(req.getProductCode().toUpperCase())
                .orElseThrow(() -> new InvalidTariffRequestException("Unknown product code: " + req.getProductCode()));

        double usedWeight = 0.0;
        double declared = req.getDeclaredValue();

        if (cat.getWeightBased()) {
            // weight must be provided in request
            if (req.getWeight() == null || req.getWeight() <= 0 ||
                Double.isNaN(req.getWeight()) || Double.isInfinite(req.getWeight()) ||
                req.getWeight() > 10000) {
                throw new InvalidTariffRequestException("Weight must be a positive, finite number less than or equal to 10,000 kg for weight-based products");
            }
            usedWeight = req.getWeight();
            // multiply declared value by weight as per Option A
            BigDecimal adjusted = BigDecimal.valueOf(declared).multiply(BigDecimal.valueOf(usedWeight));
            declared = adjusted.doubleValue();
            log.debug("Adjusted declared value by weight: {} -> {}", req.getDeclaredValue(), declared);
        } else {
            // non-weight-based: ignore weight
            usedWeight = req.getWeight() == null ? 0.0 : req.getWeight();
        }

        // Prepare service request
        TariffCalcRequest svcReq = new TariffCalcRequest();
        svcReq.originCountryCode = req.getOriginCountry();
        svcReq.destinationCountryCode = req.getDestCountry();
        svcReq.productCategoryCode = req.getProductCode();
        svcReq.declaredValue = declared;
        svcReq.date = null; // use default (today)

        // Delegate to existing TariffService which preserves calculation formulas
        TariffCalcResponse svcResp = tariffService.calculate(svcReq, false);

        CalculateTariffResponseDto resp = new CalculateTariffResponseDto();
        resp.setProductCode(cat.getCode());
        resp.setHsCode(cat.getHsCode());
        resp.setWeightBased(cat.getWeightBased());
        resp.setWeight(usedWeight);
        resp.setCalculatedTariff(svcResp.tariffAmount);
        resp.setCurrency("USD");

        return resp;
    }
}
