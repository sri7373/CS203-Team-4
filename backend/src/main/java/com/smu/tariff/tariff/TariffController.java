package com.smu.tariff.tariff;

import com.smu.tariff.tariff.dto.TariffCalcRequest;
import com.smu.tariff.tariff.dto.TariffCalcResponse;
import com.smu.tariff.tariff.dto.TariffRateDto;

import org.springframework.http.MediaType;
import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tariffs")
public class TariffController {

    private final TariffService tariffService;

    public TariffController(TariffService tariffService) {
        this.tariffService = tariffService;
    }

    //To calculate tariff based on request
    @PostMapping("/calculate")
    public ResponseEntity<TariffCalcResponse> calculate(@Valid @RequestBody TariffCalcRequest request,
                                                         @RequestParam(value = "includeSummary", defaultValue = "true") boolean includeSummary) {
        return ResponseEntity.ok(tariffService.calculate(request, includeSummary));
    }

    //To fetch tariff rates based on search criteria
    @GetMapping("/rates")
    public ResponseEntity<List<TariffRateDto>> search(@RequestParam(required = false) String origin,
                                                      @RequestParam(required = false) String destination,
                                                      @RequestParam(required = false) String category) {
        return ResponseEntity.ok(tariffService.search(origin, destination, category));
    }

    @PostMapping("/calculate/summary")
    public ResponseEntity<java.util.Map<String, String>> generateSummary(@RequestBody TariffCalcResponse response) {
        String aiSummary = tariffService.generateAiSummary(response);
        return ResponseEntity.ok(java.util.Map.of("aiSummary", aiSummary));
    }

    //To generate PDF
    @CrossOrigin(origins = "http://localhost:3000")
    @PostMapping(value = "/calculate/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> calculatePdf(@Valid @RequestBody TariffCalcRequest req) {
        //Calculate tariff
        TariffCalcResponse resp = tariffService.calculate(req);

        //Generate PDF
        byte[] pdfBytes = tariffService.generatePdfReport(resp);

        //Return PDF as response
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tariff-report.pdf")
                .contentType(MediaType.valueOf(MediaType.APPLICATION_PDF_VALUE))
                .body(pdfBytes);
    }
}
