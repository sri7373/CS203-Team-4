package com.smu.tariff.tariff;

import com.smu.tariff.tariff.dto.TariffCalcRequest;
import com.smu.tariff.tariff.dto.TariffCalcResponse;
import com.smu.tariff.tariff.dto.TariffRateDto;
import com.smu.tariff.tariff.dto.TariffRateDtoPost;
import org.springframework.http.MediaType;
import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public ResponseEntity<TariffCalcResponse> calculate(@Valid @RequestBody TariffCalcRequest request) {
        return ResponseEntity.ok(tariffService.calculate(request));
    }

    //To fetch tariff rates based on search criteria
    @GetMapping("/rates")
    public ResponseEntity<List<TariffRateDto>> search(@RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(tariffService.search(origin, destination, category));
    }

    // CREATE a new tariff rule
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TariffRateDto> create(@RequestBody @Valid TariffRateDtoPost dto) {
        return ResponseEntity.ok(tariffService.createTariff(dto));
    }

    // READ all tariff rules
    @GetMapping
    public ResponseEntity<List<TariffRateDto>> getAll() {
        return ResponseEntity.ok(tariffService.getAllTariffs());
    }

    // READ a single tariff rule by ID
    @GetMapping("/{id}")
    public ResponseEntity<TariffRateDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(tariffService.getTariffById(id));
    }

    // UPDATE a tariff rule by ID
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TariffRateDto> update(@PathVariable Long id, @RequestBody @Valid TariffRateDto dto) {
        return ResponseEntity.ok(tariffService.updateTariff(id, dto));
    }

    // DELETE a tariff rule by ID
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tariffService.deleteTariff(id);
        return ResponseEntity.noContent().build();
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
