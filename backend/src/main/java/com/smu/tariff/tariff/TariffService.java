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
import com.smu.tariff.product.ProductCategory;
import com.smu.tariff.product.ProductCategoryRepository;
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
                .orElseThrow(() -> new InvalidTariffRequestException("Unknown origin country code: " + req.originCountryCode));
        Country dest = countryRepository.findByCode(req.destinationCountryCode.toUpperCase())
                .orElseThrow(() -> new InvalidTariffRequestException("Unknown destination country code: " + req.destinationCountryCode));
        ProductCategory cat = productCategoryRepository.findByCode(req.productCategoryCode.toUpperCase())
                .orElseThrow(() -> new InvalidTariffRequestException("Unknown product category code: " + req.productCategoryCode));

        List<TariffRate> rates = tariffRateRepository.findApplicableRates(origin, dest, cat, date);
        if (rates.isEmpty()) {
            throw new TariffNotFoundException(
                String.format("No applicable tariff rate found for route %s -> %s, category %s on %s",
                    req.originCountryCode, req.destinationCountryCode, req.productCategoryCode, date));
        }
        TariffRate rate = rates.get(0);

        if ((rate.getBaseRate() == null || rate.getBaseRate().compareTo(BigDecimal.ZERO) == 0)
                && (rate.getAdditionalFee() == null || rate.getAdditionalFee().compareTo(BigDecimal.ZERO) == 0)) {
            tariffRateRepository.findFirstByProductCategoryAndBaseRateGreaterThanOrderByEffectiveFromDesc(cat, BigDecimal.ZERO)
                    .ifPresent(found -> {
                        logger.info("Using fallback rate {} for category {}", found.getId(), cat.getCode());
                        rate.setBaseRate(found.getBaseRate());
                        rate.setAdditionalFee(found.getAdditionalFee());
                    });
        }

        BigDecimal declared = BigDecimal.valueOf(req.declaredValue);
        BigDecimal baseRate = rate.getBaseRate() != null ? rate.getBaseRate() : BigDecimal.ZERO;
        BigDecimal additionalFee = rate.getAdditionalFee() != null ? rate.getAdditionalFee() : BigDecimal.ZERO;

        BigDecimal tariffAmount = declared.multiply(baseRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = declared.add(tariffAmount).add(additionalFee).setScale(2, RoundingMode.HALF_UP);

        TariffCalcResponse resp = new TariffCalcResponse();
        resp.originCountryCode = origin.getCode();
        resp.destinationCountryCode = dest.getCode();
        resp.productCategoryCode = cat.getCode();
        resp.effectiveDate = date.toString();
        resp.declaredValue = declared;
        resp.baseRate = baseRate;
        resp.tariffAmount = tariffAmount;
        resp.additionalFee = additionalFee;
        resp.totalCost = total;
        resp.notes = "Total = declaredValue + (declaredValue * baseRate) + additionalFee";
        resp.aiSummary = null; // to be filled later

        queryLogService.log(
            "CALCULATE",
            String.format("{origin:%s,dest:%s,cat:%s,val:%s,date:%s}",
                    resp.originCountryCode,
                    resp.destinationCountryCode,
                    resp.productCategoryCode,
                    declared,
                    date),
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
            - Product category: %s
            - Effective date: %s
            - Declared value (USD): %s
            - Base rate: %s
            - Calculated tariff amount: %s
            - Additional fee: %s
            - Total landed cost: %s

            Focus on likely trade policies, agreements, or market dynamics that would justify the base rate and additional fee. Provide one actionable insight importers can use to manage this tariff exposure. Respond only with HTML consisting of exactly two <p> elements and use <b> tags for emphasis. Do not use Markdown.
            """,
            resp.originCountryCode,
            resp.destinationCountryCode,
            resp.productCategoryCode,
            resp.effectiveDate,
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
            document.add(new Paragraph("Product Category: " + resp.productCategoryCode));
            document.add(new Paragraph("Effective Date: " + resp.effectiveDate));
            document.add(new Paragraph("Declared Value: " + resp.declaredValue));
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

        return new TariffRate(
                origin,
                dest,
                cat,
                dto.baseRate,
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
        dto.baseRate = rate.getBaseRate();
        dto.additionalFee = rate.getAdditionalFee();
        dto.effectiveFrom = rate.getEffectiveFrom();
        dto.effectiveTo = rate.getEffectiveTo();
        return dto;
    }
}



