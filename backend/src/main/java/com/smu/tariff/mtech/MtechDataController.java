package com.smu.tariff.mtech;

 
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mtech")
public class MtechDataController {
    private final MtechDataImporterService importerService;

    public MtechDataController(MtechDataImporterService importerService) {
        this.importerService = importerService;
    }

    @PostMapping("/import")
    public ResponseEntity<ImportResultDto> importFromUrl(@RequestBody ImportRequestDto req) {
        if (req == null || req.getUrl() == null || req.getUrl().isBlank()) {
            return ResponseEntity.badRequest().body(new ImportResultDto(0,0,"Missing url"));
        }

        ImportResultDto result = importerService.importFromUrl(req.getUrl());
        return ResponseEntity.ok(result);
    }
}
