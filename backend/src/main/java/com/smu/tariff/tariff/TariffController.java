package com.smu.tariff.tariff;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smu.tariff.tariff.dto.TariffCalcRequest;
import com.smu.tariff.tariff.dto.TariffCalcResponse;
import com.smu.tariff.tariff.dto.TariffRateDto;
import com.smu.tariff.tariff.dto.TariffRateDtoPost;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tariffs")
public class TariffController {

    private final TariffService tariffService;

    public TariffController(TariffService tariffService) {
        this.tariffService = tariffService;
    }

    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<TariffCalcResponse> calculate(@Valid @RequestBody TariffCalcRequest request,
                                                         @RequestParam(value = "includeSummary", defaultValue = "true") boolean includeSummary) {
        return ResponseEntity.ok(tariffService.calculate(request, includeSummary));
    }

    @GetMapping("/rates")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<List<TariffRateDto>> search(@RequestParam(required = false) String origin,
                                                      @RequestParam(required = false) String destination,
                                                      @RequestParam(required = false) String category) {
        return ResponseEntity.ok(tariffService.search(origin, destination, category));
    }

    // Admin CRUD endpoints for tariff management
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TariffRateDto>> getAllTariffs() {
        return ResponseEntity.ok(tariffService.getAllTariffs());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TariffRateDto> createTariff(@Valid @RequestBody TariffRateDtoPost request) {
        return ResponseEntity.ok(tariffService.createTariff(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TariffRateDto> updateTariff(@PathVariable Long id, @Valid @RequestBody TariffRateDtoPost request) {
        return ResponseEntity.ok(tariffService.updateTariff(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTariff(@PathVariable Long id) {
        tariffService.deleteTariff(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/calculate/summary")
    public ResponseEntity<java.util.Map<String, String>> generateSummary(@RequestBody TariffCalcResponse response) {
        String aiSummary = tariffService.generateAiSummary(response);
        return ResponseEntity.ok(java.util.Map.of("aiSummary", aiSummary));
    }

    //To generate PDF
    @CrossOrigin(origins = "http://localhost:3000")
    @PostMapping(value = "/calculate/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<byte[]> calculatePdf(@Valid @RequestBody TariffCalcRequest req) {
        TariffCalcResponse resp = tariffService.calculate(req);
        byte[] pdfBytes = tariffService.generatePdfReport(resp);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tariff-report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
