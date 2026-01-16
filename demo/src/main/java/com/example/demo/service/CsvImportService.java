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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.CsvImportResult;
import com.example.demo.model.Alert;
import com.example.demo.model.EventType;
import com.example.demo.model.StatusType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CsvImportService {
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
        List<String> errors = new ArrayList<>();
        List<Alert> validAlerts = new ArrayList<>();
        List<Long> createdAlertIds = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, createCsvFormat())) {

            for (CSVRecord csvRecord : csvParser) {
                try {
                    Alert alert = processCsvRecord(csvRecord);
                    validAlerts.add(alert);
                } catch (Exception e) {
                    errors.add("Строка " + csvRecord.getRecordNumber() + ": " + e.getMessage());
                    log.warn("Ошибка обработки строки {}: {}", csvRecord.getRecordNumber(), e.getMessage());
                }
            }
            
            for (Alert alert : validAlerts) {
                try {
                    Alert savedAlert = alertService.create(alert);
                    createdAlertIds.add(savedAlert.getId());
                    log.info("Успешно сохранен инцидент ID: {}, автобус: {}, тип: {}", 
                            savedAlert.getId(), savedAlert.getBusId(), savedAlert.getType());
                } catch (Exception e) {
                    errors.add("Не удалось сохранить инцидент: автобус " + alert.getBusId() + 
                              ", тип " + alert.getType() + " - " + e.getMessage());
                    log.error("Ошибка сохранения инцидента: автобус {}, тип {}", 
                             alert.getBusId(), alert.getType(), e);
                }
            }

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
        validateRequiredField(csvRecord, "bus_id", "ID автобуса");
        validateRequiredField(csvRecord, "type", "Тип инцидента");
        validateRequiredField(csvRecord, "location", "Местоположение");
        validateRequiredField(csvRecord, "description", "Описание");

        Alert alert = new Alert();

        try {
            alert.setBusId(Long.parseLong(csvRecord.get("bus_id").trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некорректный ID автобуса: " + csvRecord.get("bus_id"));
        }

        try {
            String typeStr = csvRecord.get("type").trim().toUpperCase();
            alert.setType(EventType.valueOf(typeStr));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Некорректный тип инцидента: " + csvRecord.get("type") + 
                                             ". Допустимые значения: ACCIDENT, HARD_BRAKING, BUTTON");
        }

        alert.setLocation(csvRecord.get("location").trim());
        alert.setDescription(csvRecord.get("description").trim());

        if (csvRecord.isSet("status") && !csvRecord.get("status").trim().isEmpty()) {
            try {
                String statusStr = csvRecord.get("status").trim().toUpperCase();
                alert.setStatus(StatusType.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Некорректный статус: " + csvRecord.get("status") +
                                                 ". Допустимые значения: NEW, IN_PROGRESS, RESOLVED");
            }
        } else {
            alert.setStatus(StatusType.NEW);
        }

        if (csvRecord.isSet("assigned_to_user_id") && !csvRecord.get("assigned_to_user_id").trim().isEmpty()) {
            try {
                alert.setAssignedToUserId(Long.parseLong(csvRecord.get("assigned_to_user_id").trim()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Некорректный ID пользователя: " + csvRecord.get("assigned_to_user_id"));
            }
        }

        return alert;
    }

    private void validateRequiredField(CSVRecord csvRecord, String fieldName, String fieldDescription) {
        if (!csvRecord.isSet(fieldName) || csvRecord.get(fieldName).trim().isEmpty()) {
            throw new IllegalArgumentException(fieldDescription + " не может быть пустым");
        }
    }
}