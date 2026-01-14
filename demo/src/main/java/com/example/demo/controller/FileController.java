package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.service.FileService;
import com.example.demo.model.Alert;
import com.example.demo.service.AlertService;

import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class FileController {
    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private final FileService fileService;
    private final AlertService alertService;

    @PostMapping(value = "/api/alerts/{id}/upload",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file, 
            @PathVariable Long id) {

        log.debug("Запрос загрузки файла - ID инцидента: {}, имя файла: {}, размер: {} байт", 
                 id, file.getOriginalFilename(), file.getSize());
        
        try {
            // Проверяем существование инцидента
            if (!alertService.findById(id).isPresent()) {
                log.warn("Загрузка файла не удалась: инцидент не найден - id={}", id);
                return ResponseEntity.badRequest().body("Инцидент не найден");
            }

            String resultFile = fileService.storeFile(file);
            log.debug("Файл успешно сохранен: {}", resultFile);
            
            Alert updatedAlert = alertService.addFileToAlert(id, resultFile);
            log.info("Файл успешно загружен - ID инцидента: {}, имя файла: {}", 
                    id, resultFile);

            return ResponseEntity.ok().body(new UploadResponse(
                true, 
                "Файл успешно загружен", 
                resultFile,
                updatedAlert
            ));

        } catch (IOException e) {
            log.error("Ошибка загрузки файла - ID инцидента: {}, ошибка: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().body(new UploadResponse(
                false, 
                "Ошибка загрузки файла: " + e.getMessage(), 
                null,
                null
            ));
        } catch (Exception e) {
            log.error("Непредвиденная ошибка при загрузке файла - ID инцидента: {}, ошибка: {}", 
                     id, e.getMessage(), e);
            return ResponseEntity.badRequest().body(new UploadResponse(
                false, 
                "Ошибка: " + e.getMessage(), 
                null,
                null
            ));
        }
    }

    public static record UploadResponse(
        boolean success, 
        String message, 
        String fileName,
        Alert alert
    ) {}
}