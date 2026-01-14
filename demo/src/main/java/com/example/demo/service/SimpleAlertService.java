package com.example.demo.service;

import com.example.demo.exception.AlertNotFoundException;
import com.example.demo.model.Alert;
import com.example.demo.enums.StatusType;
import com.example.demo.repository.AlertRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class SimpleAlertService implements AlertService {
    private static final Logger log = LoggerFactory.getLogger(SimpleAlertService.class);

    private final AlertRepository alertRepository;

    public SimpleAlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @Override
    public List<Alert> findAll() {
        log.debug("Получение всех инцидентов (простой сервис)");
        
        List<Alert> alerts = alertRepository.findAll();
        log.info("Получено {} инцидентов из базы данных (простой сервис)", alerts.size());
        
        return alerts;
    }

    @Override
    public List<Alert> findByStatus(StatusType status) {
        log.debug("Получение инцидентов по статусу: {} (простой сервис)", status);
        
        List<Alert> alerts = alertRepository.findByStatus(status);
        log.info("Получено {} инцидентов со статусом: {} (простой сервис)", alerts.size(), status);
        
        return alerts;
    }

    @Override
    public Optional<Alert> findById(Long id) {
        log.debug("Получение инцидента по ID: {} (простой сервис)", id);
        
        Optional<Alert> alert = alertRepository.findById(id);
        if (alert.isPresent()) {
            log.debug("Инцидент найден: id={}, тип={} (простой сервис)", id, alert.get().getType());
        } else {
            log.debug("Инцидент не найден: id={} (простой сервис)", id);
        }
        
        return alert;
    }

    @Override
    public Alert create(Alert alert) {
        log.info("Создание нового инцидента (простой сервис): busId={}, тип={}", 
                alert.getBusId(), alert.getType());
        
        if (alert.getTimestamp() == null) {
            alert.setTimestamp(java.time.LocalDateTime.now());
            log.debug("Установлено время по умолчанию для инцидента (простой сервис)");
        }
        if (alert.getStatus() == null) {
            alert.setStatus(StatusType.NEW);
            log.debug("Установлен статус NEW по умолчанию для инцидента (простой сервис)");
        }
        
        Alert savedAlert = alertRepository.save(alert);
        log.info("Инцидент успешно создан (простой сервис): id={}, busId={}", 
                savedAlert.getId(), savedAlert.getBusId());
        
        return savedAlert;
    }

    @Override
    public Alert updateStatus(Long alertId, StatusType newStatus) {
        log.info("Обновление статуса инцидента (простой сервис): id={}, новый статус={}", alertId, newStatus);
        
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> {
                    log.error("Инцидент не найден для обновления статуса (простой сервис): id={}", alertId);
                    return new AlertNotFoundException(alertId);
                });
        
        StatusType oldStatus = alert.getStatus();
        alert.setStatus(newStatus);
        
        Alert updatedAlert = alertRepository.save(alert);
        log.info("Статус инцидента обновлен (простой сервис): id={}, старый статус={}, новый статус={}", 
                alertId, oldStatus, newStatus);
        
        return updatedAlert;
    }

    @Override
    public Alert assignToUser(Long alertId, Long userId) {
        log.info("Назначение инцидента пользователю (простой сервис): инцидентId={}, пользовательId={}", alertId, userId);
        
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> {
                    log.error("Инцидент не найден для назначения (простой сервис): id={}", alertId);
                    return new AlertNotFoundException(alertId);
                });
        
        alert.setAssignedToUserId(userId);
        alert.setStatus(StatusType.IN_PROGRESS);
        
        Alert updatedAlert = alertRepository.save(alert);
        log.info("Инцидент назначен (простой сервис): инцидентId={}, пользовательId={}", alertId, userId);
        
        return updatedAlert;
    }

    @Override
    public void deleteById(Long id) {
        log.info("Удаление инцидента (простой сервис): id={}", id);
        
        alertRepository.deleteById(id);
        log.info("Инцидент успешно удален (простой сервис): id={}", id);
    }

    @Override
    public Alert addFileToAlert(Long alertId, String filePath) {
        log.info("Добавление файла к инциденту (простой сервис): инцидентId={}, путь к файлу={}", alertId, filePath);
        
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> {
                    log.error("Инцидент не найден для прикрепления файла (простой сервис): id={}", alertId);
                    return new AlertNotFoundException(alertId);
                });
        
        alert.setFilePath(filePath);
        
        Alert updatedAlert = alertRepository.save(alert);
        log.info("Файл добавлен к инциденту (простой сервис): инцидентId={}, путь к файлу={}", alertId, filePath);
        
        return updatedAlert;
    }
}