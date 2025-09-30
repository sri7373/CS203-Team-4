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
    public ResponseEntity<TariffCalcResponse> calculate(@Valid @RequestBody TariffCalcRequest request) {
        return ResponseEntity.ok(tariffService.calculate(request));
    }

    @GetMapping("/rates")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<List<TariffRateDto>> search(@RequestParam(required = false) String origin,
                                                      @RequestParam(required = false) String destination,
                                                      @RequestParam(required = false) String category) {
        return ResponseEntity.ok(tariffService.search(origin, destination, category));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TariffRateDto> create(@RequestBody @Valid TariffRateDtoPost dto) {
        return ResponseEntity.ok(tariffService.createTariff(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<List<TariffRateDto>> getAll() {
        return ResponseEntity.ok(tariffService.getAllTariffs());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<TariffRateDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(tariffService.getTariffById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TariffRateDto> update(@PathVariable Long id, @RequestBody @Valid TariffRateDto dto) {
        return ResponseEntity.ok(tariffService.updateTariff(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tariffService.deleteTariff(id);
        return ResponseEntity.noContent().build();
    }

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
