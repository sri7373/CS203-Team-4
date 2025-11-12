package com.smu.tariff.tariff;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.smu.tariff.country.Country;
import com.smu.tariff.country.CountryRepository;
import com.smu.tariff.exception.InvalidTariffRequestException;
import com.smu.tariff.exception.TariffNotFoundException;
import com.smu.tariff.logging.QueryLogService;
import com.smu.tariff.model.ProductCategory;
import com.smu.tariff.repository.ProductCategoryRepository;
import com.smu.tariff.model.TariffRate;
import com.smu.tariff.repository.TariffRateRepository;
import com.smu.tariff.tariff.dto.TariffCalcRequest;
import com.smu.tariff.tariff.dto.TariffCalcResponse;
import com.smu.tariff.tariff.dto.TariffRateDto;
import com.smu.tariff.tariff.dto.TariffRateDtoPost;
import com.smu.tariff.user.User;
import com.smu.tariff.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import com.smu.tariff.ai.GeminiClient;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

@Service
@Transactional
public class TariffService {

    private static final Logger logger = LoggerFactory.getLogger(TariffService.class);

    private final TariffRateRepository tariffRateRepository;
    private final CountryRepository countryRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final QueryLogService queryLogService;
    private static final Safelist AI_SUMMARY_SAFE_LIST = Safelist.none().addTags("p", "b");
    private final GeminiClient geminiClient;

    public TariffService(TariffRateRepository tariffRateRepository,
                         CountryRepository countryRepository,
                         ProductCategoryRepository productCategoryRepository,
                         QueryLogService queryLogService,
                         GeminiClient geminiClient) {
        this.tariffRateRepository = tariffRateRepository;
        this.countryRepository = countryRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.queryLogService = queryLogService;
        this.geminiClient = geminiClient;
    }

    public TariffCalcResponse calculate(TariffCalcRequest req) {
        return calculate(req, true);
    }

    public TariffCalcResponse calculate(TariffCalcRequest req, boolean includeSummary) {
        if (req.declaredValue == null || req.declaredValue <= 0) {
            throw new InvalidTariffRequestException("Declared value must be greater than 0");
        }
        if (req.originCountryCode == null || req.originCountryCode.trim().isEmpty()) {
            throw new InvalidTariffRequestException("Origin country code is required");
        }
        if (req.destinationCountryCode == null || req.destinationCountryCode.trim().isEmpty()) {
            throw new InvalidTariffRequestException("Destination country code is required");
        }
        if (req.productCategoryCode == null || req.productCategoryCode.trim().isEmpty()) {
            throw new InvalidTariffRequestException("Product category code is required");
        }
        if (req.hsCode == null || req.hsCode.trim().isEmpty()) {
            throw new InvalidTariffRequestException("HS code is required");
        }

        LocalDate requestedFrom = parseIsoDate(req.effectiveFrom);
        LocalDate requestedTo = parseIsoDate(req.effectiveTo);
        if (requestedFrom != null && requestedTo != null && requestedFrom.isAfter(requestedTo)) {
            throw new InvalidTariffRequestException("effectiveFrom cannot be later than effectiveTo");
        }

        Country origin = countryRepository.findByCode(req.originCountryCode.toUpperCase())
                .orElseThrow(() -> new InvalidTariffRequestException("Unknown origin country code: " + req.originCountryCode));
        Country dest = countryRepository.findByCode(req.destinationCountryCode.toUpperCase())
                .orElseThrow(() -> new InvalidTariffRequestException("Unknown destination country code: " + req.destinationCountryCode));

        ProductCategory cat = resolveCategory(req.productCategoryCode, req.hsCode);
        boolean weightBased = cat.getWeightBased();

        BigDecimal weightQuantity = null;
        if (weightBased) {
            if (req.weight == null || req.weight <= 0 || Double.isNaN(req.weight) || Double.isInfinite(req.weight)) {
                throw new InvalidTariffRequestException("Weight must be provided for weight-based products and must be positive");
            }
            if (req.weight > 10000) {
                throw new InvalidTariffRequestException("Weight cannot exceed 10,000 kg");
            }
            weightQuantity = BigDecimal.valueOf(req.weight);
        } else if (req.weight != null && req.weight < 0) {
            throw new InvalidTariffRequestException("Weight cannot be negative");
        } else if (req.weight != null && req.weight > 0) {
            weightQuantity = BigDecimal.valueOf(req.weight);
        }

        LocalDate evaluationDate = requestedFrom != null
                ? requestedFrom
                : (requestedTo != null ? requestedTo : LocalDate.now());

        List<TariffRate> rates = tariffRateRepository.findApplicableRates(origin, dest, cat, evaluationDate);
        if (rates.isEmpty()) {
            throw new TariffNotFoundException(
                    String.format("No applicable tariff rate found for route %s -> %s, HS %s on %s",
                            req.originCountryCode, req.destinationCountryCode, req.hsCode, evaluationDate));
        }

        TariffRate rate = selectRateForWindow(rates, requestedFrom, requestedTo);

        if ((rate.getBaseRate() == null || rate.getBaseRate().compareTo(BigDecimal.ZERO) == 0)
                && (rate.getAdditionalFee() == null || rate.getAdditionalFee().compareTo(BigDecimal.ZERO) == 0)) {
            tariffRateRepository.findFirstByProductCategoryAndBaseRateGreaterThanOrderByEffectiveFromDesc(cat, BigDecimal.ZERO)
                    .ifPresent(found -> {
                        logger.info("Using fallback rate {} for category {}", found.getId(), cat.getCode());
                        rate.setBaseRate(found.getBaseRate());
                        rate.setAdditionalFee(found.getAdditionalFee());
                    });
        }

        BigDecimal declaredPerUnit = BigDecimal.valueOf(req.declaredValue).setScale(2, RoundingMode.HALF_UP);
        BigDecimal declared = declaredPerUnit;
        if (weightBased && weightQuantity != null) {
            declared = declaredPerUnit.multiply(weightQuantity).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal baseRate = BaseRateUtils.fromStoredPercentage(rate.getBaseRate());
        if (baseRate == null) {
            baseRate = BigDecimal.ZERO;
        }
        BigDecimal additionalFee = rate.getAdditionalFee() != null ? rate.getAdditionalFee() : BigDecimal.ZERO;

        BigDecimal tariffAmount = declared.multiply(baseRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = declared.add(tariffAmount).add(additionalFee).setScale(2, RoundingMode.HALF_UP);

        TariffCalcResponse resp = new TariffCalcResponse();
        resp.originCountryCode = origin.getCode();
        resp.destinationCountryCode = dest.getCode();
        resp.hsCode = cat.getHsCode();
        resp.productCategoryCode = cat.getCode();
        resp.productCategoryName = cat.getName();
        resp.weightBased = weightBased;
        resp.weight = weightQuantity != null ? weightQuantity.doubleValue() : null;
        resp.rateEffectiveFrom = rate.getEffectiveFrom() != null ? rate.getEffectiveFrom().toString() : null;
        resp.rateEffectiveTo = rate.getEffectiveTo() != null ? rate.getEffectiveTo().toString() : null;
        resp.requestedEffectiveFrom = requestedFrom != null ? requestedFrom.toString() : null;
        resp.requestedEffectiveTo = requestedTo != null ? requestedTo.toString() : null;
        resp.effectiveDate = evaluationDate.toString();
        resp.declaredValuePerUnit = declaredPerUnit;
        resp.declaredValue = declared;
        resp.baseRate = BaseRateUtils.toStoredPercentage(baseRate);
        resp.tariffAmount = tariffAmount;
        resp.additionalFee = additionalFee;
        resp.totalCost = total;
        resp.notes = weightBased
                ? "Total = (declaredValuePerUnit * weight) + (weightedValue * (baseRate / 100)) + additionalFee"
                : "Total = declaredValue + (declaredValue * (baseRate / 100)) + additionalFee";
        resp.aiSummary = null; // to be filled later

        queryLogService.log(
            "CALCULATE",
            String.format("{origin:%s,destination:%s,hs:%s,category:%s,declared:%s,weight:%s,requestedFrom:%s,requestedTo:%s}",
                    resp.originCountryCode,
                    resp.destinationCountryCode,
                    req.hsCode,
                    cat.getCode(),
                    declaredPerUnit,
                    weightQuantity != null ? weightQuantity : "-",
                    resp.requestedEffectiveFrom == null ? "-" : resp.requestedEffectiveFrom,
                    resp.requestedEffectiveTo == null ? "-" : resp.requestedEffectiveTo),
            resp,
            resp.originCountryCode,
            resp.destinationCountryCode
        );

        if (includeSummary) {
            String prompt = buildAiPrompt(resp);

            try {
                String aiSummary = geminiClient.generateSummary(prompt);
                resp.aiSummary = normalizeAiSummary(aiSummary);
            } catch (Exception e) {
                logger.warn("Failed to generate AI summary", e);
                resp.aiSummary = "AI summary unavailable.";
            }
        } else {
            resp.aiSummary = null;
        }

        return resp;
    }

    private ProductCategory resolveCategory(String categoryCode, String hsCode) {
        ProductCategory fromCode = null;
        if (categoryCode != null && !categoryCode.trim().isEmpty()) {
            fromCode = productCategoryRepository.findByCode(categoryCode.trim().toUpperCase())
                    .orElseThrow(() -> new InvalidTariffRequestException("Unknown product category code: " + categoryCode));
        }

        ProductCategory fromHs = productCategoryRepository.findByHsCodeIgnoreCase(hsCode.trim())
                .orElse(null);

        if (fromCode != null && fromHs != null && !fromCode.getId().equals(fromHs.getId())) {
            throw new InvalidTariffRequestException("HS code " + hsCode + " does not match product category " + categoryCode);
        }

        if (fromCode != null) {
            return fromCode;
        }
        if (fromHs != null) {
            return fromHs;
        }
        throw new InvalidTariffRequestException("Unknown HS code: " + hsCode);
    }

    private LocalDate parseIsoDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (Exception ex) {
            throw new InvalidTariffRequestException("Invalid date format (expected yyyy-MM-dd): " + raw);
        }
    }

    private TariffRate selectRateForWindow(List<TariffRate> rates, LocalDate from, LocalDate to) {
        if (rates == null || rates.isEmpty()) {
            throw new TariffNotFoundException("No tariff rates available for supplied filters");
        }
        return rates.stream()
                .filter(rate -> overlapsRequestedWindow(rate, from, to))
                .findFirst()
                .orElse(rates.get(0));
    }

    private boolean overlapsRequestedWindow(TariffRate rate, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return true;
        }
        LocalDate windowStart = from != null ? from : LocalDate.MIN;
        LocalDate windowEnd = to != null ? to : LocalDate.MAX;
        LocalDate rateStart = rate.getEffectiveFrom() != null ? rate.getEffectiveFrom() : LocalDate.MIN;
        LocalDate rateEnd = rate.getEffectiveTo() != null ? rate.getEffectiveTo() : LocalDate.MAX;
        return !rateStart.isAfter(windowEnd) && !rateEnd.isBefore(windowStart);
    }

    public String generateAiSummary(TariffCalcResponse resp) {
        if (resp == null) {
            throw new IllegalArgumentException("Tariff response is required");
        }

        String prompt = buildAiPrompt(resp);
        try {
            String aiSummary = geminiClient.generateSummary(prompt);
            return normalizeAiSummary(aiSummary);
        } catch (Exception e) {
            logger.warn("Failed to generate AI summary", e);
            return "AI summary unavailable.";
        }
    }


    private String normalizeAiSummary(String raw) {
        if (raw == null) {
            return null;
        }

        String content = raw.trim();
        if (content.isEmpty()) {
            return "";
        }

        content = content.replace("\r\n", "\n");
        content = content.replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>");

        if (!content.toLowerCase().contains("<p")) {
            String[] paragraphs = content.split("\n{2,}");
            content = java.util.Arrays.stream(paragraphs)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(p -> p.replace("\n", " "))
                    .map(p -> p.replaceAll("\\s+", " "))
                    .map(p -> "<p>" + p.trim() + "</p>")
                    .collect(java.util.stream.Collectors.joining());
        }

        String sanitized = Jsoup.clean(content, AI_SUMMARY_SAFE_LIST);
        return sanitized.replace("\n", "").trim();
    }

    private String buildAiPrompt(TariffCalcResponse resp) {
        return String.format("""
            You are an international trade analyst. In fewer than 120 words, explain why the following tariff structure could be in place.

            Tariff inputs:
            - Origin country: %s
            - Destination country: %s
            - HS code: %s
            - Product category: %s
            - Tariff schedule window: %s to %s
            - Requested period: %s to %s
            - Declared value per unit (USD): %s
            - Weight applied (kg): %s
            - Weighted declared value (USD): %s
            - Base rate: %s
            - Calculated tariff amount: %s
            - Additional fee: %s
            - Total landed cost: %s

            Focus on likely trade policies, agreements, or market dynamics that would justify the base rate and additional fee. Provide one actionable insight importers can use to manage this tariff exposure. Respond only with HTML consisting of exactly two <p> elements and use <b> tags for emphasis. Do not use Markdown.
            """,
            resp.originCountryCode,
            resp.destinationCountryCode,
            resp.hsCode == null ? "-" : resp.hsCode,
            resp.productCategoryCode,
            resp.rateEffectiveFrom == null ? "n/a" : resp.rateEffectiveFrom,
            resp.rateEffectiveTo == null ? "open" : resp.rateEffectiveTo,
            resp.requestedEffectiveFrom == null ? "n/a" : resp.requestedEffectiveFrom,
            resp.requestedEffectiveTo == null ? "n/a" : resp.requestedEffectiveTo,
            resp.declaredValuePerUnit.toPlainString(),
            resp.weight == null ? "n/a" : resp.weight.toString(),
            resp.declaredValue.toPlainString(),
            resp.baseRate.toPlainString(),
            resp.tariffAmount.toPlainString(),
            resp.additionalFee.toPlainString(),
            resp.totalCost.toPlainString()
        );
    }

    public List<TariffRateDto> search(String originCode, String destCode, String catCode) {
        Country origin = null;
        Country dest = null;
        ProductCategory cat = null;

        if (originCode != null && !originCode.trim().isEmpty()) {
            origin = countryRepository.findByCode(originCode.toUpperCase())
                    .orElseThrow(() -> new InvalidTariffRequestException("Unknown origin country code: " + originCode));
        }
        if (destCode != null && !destCode.trim().isEmpty()) {
            dest = countryRepository.findByCode(destCode.toUpperCase())
                    .orElseThrow(() -> new InvalidTariffRequestException("Unknown destination country code: " + destCode));
        }
        if (catCode != null && !catCode.trim().isEmpty()) {
            cat = productCategoryRepository.findByCode(catCode.toUpperCase())
                    .orElseThrow(() -> new InvalidTariffRequestException("Unknown product category code: " + catCode));
        }

        List<TariffRateDto> dtos = tariffRateRepository.search(origin, dest, cat)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        var resultSummary = new java.util.HashMap<String, Object>();
        resultSummary.put("count", dtos.size());
        var sampleIds = dtos.stream()
                .map(dto -> dto.id)
                .filter(id -> id != null)
                .limit(3)
                .collect(Collectors.toList());
        if (!sampleIds.isEmpty()) {
            resultSummary.put("sampleIds", sampleIds);
        }

        queryLogService.log(
            "SEARCH",
            String.format("{origin:%s,dest:%s,cat:%s}", originCode, destCode, catCode),
            resultSummary,
            originCode,
            destCode
        );

        return dtos;
    }

    public TariffRateDto createTariff(TariffRateDtoPost dto) {
        TariffRate rate = buildTariffFromPostDto(dto);
        TariffRate saved = tariffRateRepository.save(rate);

        queryLogService.log(
            "CREATE_TARIFF",
            summarizeTariff(rate),
            mapToDto(saved),
            saved.getOrigin().getCode(),
            saved.getDestination().getCode()
        );

        return mapToDto(saved);
    }

    public List<TariffRateDto> getAllTariffs() {
        return tariffRateRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public TariffRateDto getTariffById(Long id) {
        TariffRate rate = tariffRateRepository.findById(id)
                .orElseThrow(() -> new TariffNotFoundException("Tariff with id " + id + " not found"));
        return mapToDto(rate);
    }

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
        if (dto.baseRate != null) {
            rate.setBaseRate(dto.baseRate);
        }
        if (dto.additionalFee != null) {
            rate.setAdditionalFee(dto.additionalFee);
        }
        if (dto.effectiveFrom != null) {
            rate.setEffectiveFrom(dto.effectiveFrom);
        }
        if (dto.effectiveTo != null) {
            rate.setEffectiveTo(dto.effectiveTo);
        }

        TariffRate saved = tariffRateRepository.save(rate);

        queryLogService.log(
            "UPDATE_TARIFF", summarizeTariff(saved),
            mapToDto(saved),
            saved.getOrigin().getCode(),
            saved.getDestination().getCode()
        );

        return mapToDto(saved);
    }

    // Overload for TariffRateDtoPost (used by admin create/update endpoints)
    public TariffRateDto updateTariff(Long id, TariffRateDtoPost dto) {
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
        if (dto.baseRate != null) {
            rate.setBaseRate(dto.baseRate);
        }
        if (dto.additionalFee != null) {
            rate.setAdditionalFee(dto.additionalFee);
        }
        if (dto.effectiveFrom != null) {
            rate.setEffectiveFrom(dto.effectiveFrom);
        }
        rate.setEffectiveTo(dto.effectiveTo);

        TariffRate saved = tariffRateRepository.save(rate);

        queryLogService.log(
            "UPDATE_TARIFF", summarizeTariff(saved),
            mapToDto(saved),
            saved.getOrigin().getCode(),
            saved.getDestination().getCode()
        );

        return mapToDto(saved);
    }

    public void deleteTariff(Long id) {
        TariffRate rate = tariffRateRepository.findById(id)
                .orElseThrow(() -> new TariffNotFoundException("Tariff with id " + id + " not found"));
        tariffRateRepository.delete(rate);

        queryLogService.log(
            "DELETE_TARIFF", summarizeTariff(rate),
            null,
            rate.getOrigin().getCode(),
            rate.getDestination().getCode()
        );
    }

    public byte[] generatePdfReport(TariffCalcResponse resp) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("Tariff Calculation Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            document.add(new Paragraph("Generated on: " + LocalDate.now()));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Origin Country: " + resp.originCountryCode));
            document.add(new Paragraph("Destination Country: " + resp.destinationCountryCode));
            document.add(new Paragraph("HS Code: " + (resp.hsCode == null ? "-" : resp.hsCode)));
            String categoryDisplay = resp.productCategoryName != null
                    ? resp.productCategoryName + " (" + resp.productCategoryCode + ")"
                    : resp.productCategoryCode;
            document.add(new Paragraph("Product Category: " + categoryDisplay));
            document.add(new Paragraph(String.format("Tariff Schedule: %s to %s",
                    resp.rateEffectiveFrom == null ? "-" : resp.rateEffectiveFrom,
                    resp.rateEffectiveTo == null ? "open-ended" : resp.rateEffectiveTo)));
            document.add(new Paragraph(String.format("Requested Window: %s to %s",
                    resp.requestedEffectiveFrom == null ? "-" : resp.requestedEffectiveFrom,
                    resp.requestedEffectiveTo == null ? "-" : resp.requestedEffectiveTo)));
            document.add(new Paragraph("Declared Value (per unit): " + resp.declaredValuePerUnit));
            if (Boolean.TRUE.equals(resp.weightBased)) {
                document.add(new Paragraph("Weight Applied (kg): " + (resp.weight == null ? "-" : resp.weight)));
            }
            document.add(new Paragraph("Weighted Declared Value: " + resp.declaredValue));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);

            table.addCell("Base Rate");
            table.addCell(resp.baseRate.toPlainString());

            table.addCell("Tariff Amount");
            table.addCell(resp.tariffAmount.toPlainString());

            table.addCell("Additional Fee");
            table.addCell(resp.additionalFee.toPlainString());

            table.addCell("Total Cost");
            table.addCell(resp.totalCost.toPlainString());

            document.add(table);

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Notes: " + resp.notes));

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }

        return out.toByteArray();
    }

    private TariffRate buildTariffFromPostDto(TariffRateDtoPost dto) {
        if (dto.originCountryCode == null || dto.originCountryCode.isBlank() ||
            dto.destinationCountryCode == null || dto.destinationCountryCode.isBlank() ||
            dto.productCategoryCode == null || dto.productCategoryCode.isBlank()) {
            throw new InvalidTariffRequestException("Origin, destination and product category codes are required");
        }
        if (dto.baseRate == null || dto.additionalFee == null || dto.effectiveFrom == null) {
            throw new InvalidTariffRequestException("Base rate, additional fee and effective-from date are required");
        }

        Country origin = countryRepository.findByCode(dto.originCountryCode.toUpperCase())
                .orElseThrow(() -> new InvalidTariffRequestException("Unknown origin country code: " + dto.originCountryCode));
        Country dest = countryRepository.findByCode(dto.destinationCountryCode.toUpperCase())
                .orElseThrow(() -> new InvalidTariffRequestException("Unknown destination country code: " + dto.destinationCountryCode));
        ProductCategory cat = productCategoryRepository.findByCode(dto.productCategoryCode.toUpperCase())
                .orElseThrow(() -> new InvalidTariffRequestException("Unknown product category code: " + dto.productCategoryCode));

        BigDecimal storedBaseRate = dto.baseRate;

        return new TariffRate(
                origin,
                dest,
                cat,
                storedBaseRate,
                dto.additionalFee,
                dto.effectiveFrom,
                dto.effectiveTo
        );
    }

    private String summarizeTariff(TariffRate rate) {
        String idPart = rate.getId() != null ? rate.getId().toString() : "new";
        return String.format("{id:%s,origin:%s,dest:%s,cat:%s,from:%s,to:%s}",
                idPart,
                rate.getOrigin().getCode(),
                rate.getDestination().getCode(),
                rate.getProductCategory().getCode(),
                rate.getEffectiveFrom(),
                rate.getEffectiveTo());
    }

    private TariffRateDto mapToDto(TariffRate rate) {
        TariffRateDto dto = new TariffRateDto();
        dto.id = rate.getId();
        dto.originCountryCode = rate.getOrigin().getCode();
        dto.destinationCountryCode = rate.getDestination().getCode();
        dto.productCategoryCode = rate.getProductCategory().getCode();
        dto.productCategoryName = rate.getProductCategory().getName();
        dto.hsCode = rate.getProductCategory().getHsCode();
        dto.weightBased = rate.getProductCategory().getWeightBased();
        dto.weightValue = rate.getWeightValue();
        dto.baseRate = rate.getBaseRate();
        dto.additionalFee = rate.getAdditionalFee();
        dto.effectiveFrom = rate.getEffectiveFrom();
        dto.effectiveTo = rate.getEffectiveTo();
        return dto;
    }

}

