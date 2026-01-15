package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.CsvImportResult;
import com.example.demo.service.CsvImportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class CsvImportController {
    private static final Logger log = LoggerFactory.getLogger(CsvImportController.class);

    private final CsvImportService csvImportService;

    @PostMapping(value = "/import-csv", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<CsvImportResult> importCsv(@RequestParam("file") MultipartFile file) {
        log.info("Запрос импорта CSV - имя файла: {}, размер: {} байт", 
                file.getOriginalFilename(), file.getSize());
        
        try {
            if (file.isEmpty()) {
                log.warn("Импорт CSV не удался: файл пустой");
                CsvImportResult emptyResult = new CsvImportResult();
                emptyResult.setSuccessCount(0);
                emptyResult.setFailedCount(0);
                emptyResult.setErrors(java.util.List.of("Файл пустой"));
                return ResponseEntity.badRequest().body(emptyResult);
            }

            String contentType = file.getContentType();
            if (contentType == null || 
                (!contentType.equals("text/csv") && 
                 !contentType.equals("application/vnd.ms-excel") &&
                 !file.getOriginalFilename().toLowerCase().endsWith(".csv"))) {
                log.warn("Импорт CSV не удался: неверный тип файла - {}", contentType);
                CsvImportResult invalidResult = new CsvImportResult();
                invalidResult.setSuccessCount(0);
                invalidResult.setFailedCount(0);
                invalidResult.setErrors(java.util.List.of("Файл должен быть в формате CSV"));
                return ResponseEntity.badRequest().body(invalidResult);
            }

            CsvImportResult result = csvImportService.importAlertsFromCsv(file);
            
            log.info("Импорт CSV завершен - успешно: {}, неудачно: {}, ошибок: {}", 
                    result.getSuccessCount(), result.getFailedCount(), result.getErrors().size());
            
            if (result.hasError()) {
                log.warn("Импорт CSV завершен с ошибками: {}", result.getErrors());
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Ошибка импорта CSV: {}", e.getMessage(), e);
            CsvImportResult errorResult = new CsvImportResult();
            errorResult.setSuccessCount(0);
            errorResult.setFailedCount(0);
            errorResult.setErrors(java.util.List.of("Ошибка обработки файла: " + e.getMessage()));
            return ResponseEntity.badRequest().body(errorResult);
        }
    }
}