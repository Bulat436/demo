package com.example.demo.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.CsvImportResult;
import com.example.demo.model.Alert;
import com.example.demo.model.EventType;
import com.example.demo.model.StatusType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CsvImportService {
    private static final Logger log = LoggerFactory.getLogger(CsvImportService.class);
    
    private final AlertService alertService;

    private CSVFormat createCsvFormat() {
        return CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .build();
    }

    public CsvImportResult importAlertsFromCsv(MultipartFile file) {
        log.info("Начало импорта CSV файла: {}", file.getOriginalFilename());
        
        List<String> errors = new ArrayList<>();
        List<Alert> validAlerts = new ArrayList<>();
        List<Long> createdAlertIds = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, createCsvFormat())) {

            int totalRows = 0;
            for (CSVRecord csvRecord : csvParser) {
                totalRows++;
                try {
                    Alert alert = processCsvRecord(csvRecord);
                    validAlerts.add(alert);
                    log.debug("Строка {} успешно обработана", csvRecord.getRecordNumber());
                } catch (Exception e) {
                    String errorMsg = "Строка " + csvRecord.getRecordNumber() + ": " + e.getMessage();
                    errors.add(errorMsg);
                    log.warn("Ошибка обработки строки {}: {}", csvRecord.getRecordNumber(), e.getMessage());
                }
            }

            log.info("Файл прочитан, всего строк: {}, валидных: {}", totalRows, validAlerts.size());
            
            for (Alert alert : validAlerts) {
                try {
                    Alert savedAlert = alertService.create(alert);
                    createdAlertIds.add(savedAlert.getId());
                    log.info("Успешно сохранен инцидент ID: {}, автобус: {}, тип: {}", 
                            savedAlert.getId(), savedAlert.getBusId(), savedAlert.getType());
                } catch (Exception e) {
                    String errorMsg = "Не удалось сохранить инцидент: автобус " + alert.getBusId() + 
                              ", тип " + alert.getType() + " - " + e.getMessage();
                    errors.add(errorMsg);
                    log.error("Ошибка сохранения инцидента: автобус {}, тип {}", 
                             alert.getBusId(), alert.getType(), e);
                }
            }

            log.info("Импорт завершен: успешно сохранено {}, не удалось {}", 
                    createdAlertIds.size(), validAlerts.size() - createdAlertIds.size());

            return new CsvImportResult(
                createdAlertIds.size(),
                validAlerts.size() - createdAlertIds.size(),
                errors,
                createdAlertIds
            );

        } catch (IOException e) {
            log.error("Ошибка при чтении CSV файла", e);
            errors.add("Не удалось прочитать файл: " + e.getMessage());
            return new CsvImportResult(0, 0, errors, new ArrayList<>());
        }
    }

    private Alert processCsvRecord(CSVRecord csvRecord) {
        log.debug("Обработка строки CSV: {}", csvRecord.getRecordNumber());
        
        validateRequiredField(csvRecord, "bus_id", "ID автобуса");
        validateRequiredField(csvRecord, "type", "Тип инцидента");
        validateRequiredField(csvRecord, "location", "Местоположение");
        validateRequiredField(csvRecord, "description", "Описание");

        Alert alert = new Alert();

        try {
            alert.setBusId(Long.parseLong(csvRecord.get("bus_id").trim()));
            log.trace("ID автобуса установлен: {}", alert.getBusId());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некорректный ID автобуса: " + csvRecord.get("bus_id"));
        }

        try {
            String typeStr = csvRecord.get("type").trim().toUpperCase();
            alert.setType(EventType.valueOf(typeStr));
            log.trace("Тип инцидента установлен: {}", alert.getType());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Некорректный тип инцидента: " + csvRecord.get("type") + 
                                             ". Допустимые значения: ACCIDENT, HARD_BRAKING, BUTTON");
        }

        alert.setLocation(csvRecord.get("location").trim());
        alert.setDescription(csvRecord.get("description").trim());
        log.trace("Местоположение и описание установлены");

        if (csvRecord.isSet("status") && !csvRecord.get("status").trim().isEmpty()) {
            try {
                String statusStr = csvRecord.get("status").trim().toUpperCase();
                alert.setStatus(StatusType.valueOf(statusStr));
                log.trace("Статус установлен: {}", alert.getStatus());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Некорректный статус: " + csvRecord.get("status") +
                                                 ". Допустимые значения: NEW, IN_PROGRESS, RESOLVED");
            }
        } else {
            alert.setStatus(StatusType.NEW);
            log.trace("Статус установлен по умолчанию: NEW");
        }

        if (csvRecord.isSet("assigned_to_user_id") && !csvRecord.get("assigned_to_user_id").trim().isEmpty()) {
            try {
                alert.setAssignedToUserId(Long.parseLong(csvRecord.get("assigned_to_user_id").trim()));
                log.trace("Назначенный пользователь ID установлен: {}", alert.getAssignedToUserId());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Некорректный ID пользователя: " + csvRecord.get("assigned_to_user_id"));
            }
        }

        log.debug("Строка CSV успешно обработана в объект Alert");
        return alert;
    }

    private void validateRequiredField(CSVRecord csvRecord, String fieldName, String fieldDescription) {
        if (!csvRecord.isSet(fieldName) || csvRecord.get(fieldName).trim().isEmpty()) {
            throw new IllegalArgumentException(fieldDescription + " не может быть пустым");
        }
    }
}