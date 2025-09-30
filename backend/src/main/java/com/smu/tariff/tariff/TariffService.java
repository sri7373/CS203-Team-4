package com.smu.tariff.tariff;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.io.ByteArrayOutputStream;

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
import com.smu.tariff.ai.GeminiClient;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

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
        
        // Fetch entities, throw error if not found
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
        TariffRate rate = rates.get(0); // latest effective rate

        BigDecimal declared = BigDecimal.valueOf(req.declaredValue).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tariffAmount = declared.multiply(rate.getBaseRate()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = declared.add(tariffAmount).add(rate.getAdditionalFee()).setScale(2, RoundingMode.HALF_UP);

        // Prepare response
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
        resp.aiSummary = null; // to be filled later

        // Log the query
        logQuery("CALCULATE", String.format("{origin:%s,dest:%s,cat:%s,val:%s,date:%s}",
                resp.originCountryCode, resp.destinationCountryCode, resp.productCategoryCode, declared, date));

        String prompt = String.format(
             "Summarize this tariff calculation clearly in business terms using less than 100 words. " +
            "Format the response as valid HTML using <p> for paragraphs and <b> for important values or keywords. " +
            "Also include recent global news or trade policy context related to <b>%s tariffs</b> between <b>%s</b> and <b>%s</b>.\n\n" +
            "<p><b>Calculation:</b></p>" +
             "<ul>" +
            "<li>Declared Value: %s</li>" +
            "<li>Tariff: %s</li>" +
            "<li>Fee: %s</li>" +
            "<li>Total: %s</li>" +
            "</ul>",
            resp.productCategoryCode,
            resp.originCountryCode,
            resp.destinationCountryCode,
            resp.declaredValue, resp.tariffAmount, resp.additionalFee, resp.totalCost
        );
                
        try {
            GeminiClient gemini = new GeminiClient(System.getenv("GEMINI_API_KEY"));
            String aiText = gemini.generateSummary(prompt);  // already parsed text
            resp.aiSummary = aiText;
        } catch (Exception e) {
            e.printStackTrace();
            resp.aiSummary = "AI summary unavailable.";
        }
// resp.aiSummary = "AI summary unavailable."; // Temporarily disable AI feature


        return resp;
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

        logQuery("SEARCH", String.format("{origin:%s,dest:%s,cat:%s}", originCode, destCode, catCode));
        return dtos;
    }

    private void logQuery(String type, String params) {
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (auth != null && auth.getPrincipal() instanceof User) ? (User) auth.getPrincipal() : null;
        queryLogRepository.save(new QueryLog(user, type, params));
    }

    public byte[] generatePdfReport(TariffCalcResponse resp) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Title
            document.add(new Paragraph("Tariff Calculation Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            document.add(new Paragraph("Generated on: " + LocalDate.now()));
            document.add(new Paragraph(" "));

            // Input details
            document.add(new Paragraph("Origin Country: " + resp.originCountryCode));
            document.add(new Paragraph("Destination Country: " + resp.destinationCountryCode));
            document.add(new Paragraph("Product Category: " + resp.productCategoryCode));
            document.add(new Paragraph("Effective Date: " + resp.effectiveDate));
            document.add(new Paragraph("Declared Value: " + resp.declaredValue));
            document.add(new Paragraph(" "));

            // Calculation details in table
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

            // Notes
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Notes: " + resp.notes));

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }

        return out.toByteArray();
    }
}
