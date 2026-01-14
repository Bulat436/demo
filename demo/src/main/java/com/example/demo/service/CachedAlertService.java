package com.example.demo.service;

import com.example.demo.exception.AlertNotFoundException;
import com.example.demo.model.Alert;
import com.example.demo.enums.StatusType;
import com.example.demo.repository.AlertRepository;
import com.example.demo.specification.AlertSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Primary
@Transactional
public class CachedAlertService implements AlertService {
    private static final Logger log = LoggerFactory.getLogger(CachedAlertService.class);

    private final AlertRepository alertRepository;

    public CachedAlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "alerts", unless = "#result.isEmpty()")
    public List<Alert> findAll() {
        log.debug("Получение всех инцидентов (с кэшированием)");
        
        List<Alert> alerts = alertRepository.findAll();
        log.info("Получено {} инцидентов из базы данных", alerts.size());
        
        return alerts;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "alertsByStatus", key = "#status.name()")
    public List<Alert> findByStatus(StatusType status) {
        log.debug("Получение инцидентов по статусу: {} (с кэшированием)", status);
        
        List<Alert> alerts = alertRepository.findByStatus(status);
        log.info("Получено {} инцидентов со статусом: {}", alerts.size(), status);
        
        return alerts;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Alert> findById(Long id) {
        log.debug("Получение инцидента по ID: {}", id);
        
        Optional<Alert> alert = alertRepository.findById(id);
        if (alert.isPresent()) {
            log.debug("Инцидент найден: id={}, тип={}", id, alert.get().getType());
        } else {
            log.debug("Инцидент не найден: id={}", id);
        }
        
        return alert;
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "alerts", allEntries = true),
        @CacheEvict(value = "alertsByStatus", allEntries = true),
        @CacheEvict(value = "alertsByBus", allEntries = true),
        @CacheEvict(value = "alertsByUser", allEntries = true)
    })
    public Alert create(Alert alert) {
        log.info("Создание нового инцидента: busId={}, тип={}, местоположение={}", 
                alert.getBusId(), alert.getType(), alert.getLocation());
        
        if (alert.getTimestamp() == null) {
            alert.setTimestamp(java.time.LocalDateTime.now());
            log.debug("Установлено время по умолчанию для инцидента");
        }
        if (alert.getStatus() == null) {
            alert.setStatus(StatusType.NEW);
            log.debug("Установлен статус NEW по умолчанию для инцидента");
        }        
        Alert savedAlert = alertRepository.save(alert);
        log.info("Инцидент успешно создан: id={}, busId={}, тип={}, статус={}", 
                savedAlert.getId(), savedAlert.getBusId(), savedAlert.getType(), savedAlert.getStatus());
        
        log.debug("Все кэши инцидентов инвалидированы после создания");
        
        return savedAlert;
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "alerts", allEntries = true),
        @CacheEvict(value = "alertsByStatus", allEntries = true),
        @CacheEvict(value = "alertsByBus", allEntries = true),
        @CacheEvict(value = "alertsByUser", allEntries = true)
    })
    public Alert updateStatus(Long alertId, StatusType newStatus) {
        log.info("Обновление статуса инцидента: id={}, новый статус={}", alertId, newStatus);
        
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> {
                    log.error("Инцидент не найден для обновления статуса: id={}", alertId);
                    return new AlertNotFoundException(alertId);
                });
        
        StatusType oldStatus = alert.getStatus();
        alert.setStatus(newStatus);        
        Alert updatedAlert = alertRepository.save(alert);
        log.info("Статус инцидента обновлен: id={}, старый статус={}, новый статус={}", 
                alertId, oldStatus, newStatus);
        
        log.debug("Все кэши инцидентов инвалидированы после обновления статуса");
        
        return updatedAlert;
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "alerts", allEntries = true),
        @CacheEvict(value = "alertsByStatus", allEntries = true),
        @CacheEvict(value = "alertsByBus", allEntries = true),
        @CacheEvict(value = "alertsByUser", allEntries = true)
    })
    public Alert assignToUser(Long alertId, Long userId) {
        log.info("Назначение инцидента пользователю: инцидентId={}, пользовательId={}", alertId, userId);
        
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> {
                    log.error("Инцидент не найден для назначения: id={}", alertId);
                    return new AlertNotFoundException(alertId);
                });
        
        alert.setAssignedToUserId(userId);
        alert.setStatus(StatusType.IN_PROGRESS);
        
        Alert updatedAlert = alertRepository.save(alert);
        log.info("Инцидент назначен: инцидентId={}, пользовательId={}, новый статус={}", 
                alertId, userId, StatusType.IN_PROGRESS);
        
        log.debug("Все кэши инцидентов инвалидированы после назначения");
        
        return updatedAlert;
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "alerts", allEntries = true),
        @CacheEvict(value = "alertsByStatus", allEntries = true),
        @CacheEvict(value = "alertsByBus", allEntries = true),
        @CacheEvict(value = "alertsByUser", allEntries = true)
    })
    public void deleteById(Long id) {
        log.info("Удаление инцидента: id={}", id);
        
        if (!alertRepository.existsById(id)) {
            log.error("Инцидент не найден для удаления: id={}", id);
            throw new AlertNotFoundException(id);
        }
        
        alertRepository.deleteById(id);
        log.info("Инцидент успешно удален: id={}", id);
        
        log.debug("Все кэши инцидентов инвалидированы после удаления");
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "alerts", allEntries = true),
        @CacheEvict(value = "alertsByStatus", allEntries = true),
        @CacheEvict(value = "alertsByBus", allEntries = true),
        @CacheEvict(value = "alertsByUser", allEntries = true)
    })
    public Alert addFileToAlert(Long alertId, String filePath) {
        log.info("Добавление файла к инциденту: инцидентId={}, путь к файлу={}", alertId, filePath);
        
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> {
                    log.error("Инцидент не найден для прикрепления файла: id={}", alertId);
                    return new AlertNotFoundException(alertId);
                });
        
        alert.setFilePath(filePath);
        
        Alert updatedAlert = alertRepository.save(alert);
        log.info("Файл добавлен к инциденту: инцидентId={}, путь к файлу={}", alertId, filePath);
        
        log.debug("Все кэши инцидентов инвалидированы после добавления файла");
        
        return updatedAlert;
    }


    @Transactional(readOnly = true)
    @Cacheable(value = "alertsByBus", key = "#busId")
    public List<Alert> findByBusId(Long busId) {
        log.debug("Получение инцидентов по ID автобуса: {} (с кэшированием)", busId);
        
        List<Alert> alerts = alertRepository.findByBusId(busId);
        log.info("Получено {} инцидентов для автобуса ID: {}", alerts.size(), busId);
        
        return alerts;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "alertsByUser", key = "#userId")
    public List<Alert> findByAssignedToUserId(Long userId) {
        log.debug("Получение инцидентов по назначенному пользователю ID: {} (с кэшированием)", userId);
        
        List<Alert> alerts = alertRepository.findByAssignedToUserId(userId);
        log.info("Получено {} инцидентов назначенных пользователю ID: {}", alerts.size(), userId);
        
        return alerts;
    }


    @Transactional(readOnly = true)
    public List<Alert> findByFilters(StatusType status, Long busId, String location) {
        log.debug("Получение инцидентов по фильтрам: статус={}, автобусId={}, местоположение={}", 
                 status, busId, location);
        
        Specification<Alert> spec = AlertSpecification.filter(status, busId, location);
        List<Alert> alerts = alertRepository.findAll(spec);
        
        log.info("Получено {} инцидентов с использованием фильтров", alerts.size());
        
        return alerts;
    }


    @Caching(evict = {
        @CacheEvict(value = "alerts", allEntries = true),
        @CacheEvict(value = "alertsByStatus", allEntries = true),
        @CacheEvict(value = "alertsByBus", allEntries = true),
        @CacheEvict(value = "alertsByUser", allEntries = true)
    })
    public void clearAllCache() {
        
        log.info("Ручная очистка всех кэшей инцидентов");
    }
}