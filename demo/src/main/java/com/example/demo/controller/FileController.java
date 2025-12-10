package com.example.demo.controller;

import java.io.IOException;

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

@RestController
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final AlertService alertService;

    @PostMapping(value = "/api/alerts/{id}/upload",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file, 
            @PathVariable Long id) {
        
        try {
            // Проверяем существование инцидента
            if (!alertService.findById(id).isPresent()) {
                return ResponseEntity.badRequest().body("Инцидент не найден");
            }
            
            String resultFile = fileService.storeFile(file);
            Alert updatedAlert = alertService.addFileToAlert(id, resultFile);
            
            return ResponseEntity.ok().body(new UploadResponse(
                true, 
                "Файл успешно загружен", 
                resultFile,
                updatedAlert
            ));
            
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(new UploadResponse(
                false, 
                "Ошибка загрузки файла: " + e.getMessage(), 
                null,
                null
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new UploadResponse(
                false, 
                "Ошибка: " + e.getMessage(), 
                null,
                null
            ));
        }
    }
    
    // DTO для ответа
    public static record UploadResponse(
        boolean success, 
        String message, 
        String fileName,
        Alert alert
    ) {}
}