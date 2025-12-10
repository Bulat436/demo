package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

@Service
public class FileService {
    
    @Value("${upload.path:uploads}")
    private String uploadDir;
    
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        "jpg", "jpeg", "png", "gif", "bmp", "pdf", "doc", "docx"
    );

    public String storeFile(MultipartFile file) throws IOException {
        // Проверка пустого файла
        if (file.isEmpty()) {
            throw new IOException("Файл пустой");
        }
        
        // Проверка размера файла
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IOException("Размер файла превышает 5MB");
        }
        
        // Проверка расширения файла
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFileName).toLowerCase();
        
        if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
            throw new IOException("Недопустимый тип файла. Разрешены: " + ALLOWED_EXTENSIONS);
        }
        
        // Создание директории если не существует
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Генерация уникального имени файла
        String fileName = UUID.randomUUID().toString() + "." + fileExtension;
        Path targetLocation = uploadPath.resolve(fileName);
        
        // Копирование файла
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        
        return fileName;
    }
    
    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1);
        }
        return "";
    }
    
    public boolean deleteFile(String fileName) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(fileName);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }
}