package com.example.demo.service;

import com.example.demo.exception.AlertNotFoundException;
import com.example.demo.model.Alert;
import com.example.demo.model.StatusType;
import com.example.demo.repository.AlertRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CachedAlertService implements AlertService {

    private final AlertRepository alertRepository;

    public CachedAlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "alerts", unless = "#result.isEmpty()")
    public List<Alert> findAll() {
        return alertRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "alertsByStatus", key = "#status.name()")
    public List<Alert> findByStatus(StatusType status) {
        return alertRepository.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Alert> findById(Long id) {
        // Не кешируем отдельные сущности, т.к. они могут часто меняться
        return alertRepository.findById(id);
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "alerts", allEntries = true),
        @CacheEvict(value = "alertsByStatus", allEntries = true),
        @CacheEvict(value = "alertsByBus", allEntries = true),
        @CacheEvict(value = "alertsByUser", allEntries = true)
    })
    public Alert create(Alert alert) {
        if (alert.getTimestamp() == null) {
            alert.setTimestamp(java.time.LocalDateTime.now());
        }
        if (alert.getStatus() == null) {
            alert.setStatus(StatusType.NEW);
        }
        return alertRepository.save(alert);
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "alerts", allEntries = true),
        @CacheEvict(value = "alertsByStatus", allEntries = true),
        @CacheEvict(value = "alertsByBus", allEntries = true),
        @CacheEvict(value = "alertsByUser", allEntries = true)
    })
    public Alert updateStatus(Long alertId, StatusType newStatus) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new AlertNotFoundException(alertId));
        alert.setStatus(newStatus);
        return alertRepository.save(alert);
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "alerts", allEntries = true),
        @CacheEvict(value = "alertsByStatus", allEntries = true),
        @CacheEvict(value = "alertsByBus", allEntries = true),
        @CacheEvict(value = "alertsByUser", allEntries = true)
    })
    public Alert assignToUser(Long alertId, Long userId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new AlertNotFoundException(alertId));
        alert.setAssignedToUserId(userId);
        alert.setStatus(StatusType.IN_PROGRESS);
        return alertRepository.save(alert);
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "alerts", allEntries = true),
        @CacheEvict(value = "alertsByStatus", allEntries = true),
        @CacheEvict(value = "alertsByBus", allEntries = true),
        @CacheEvict(value = "alertsByUser", allEntries = true)
    })
    public void deleteById(Long id) {
        if (!alertRepository.existsById(id)) {
            throw new AlertNotFoundException(id);
        }
        alertRepository.deleteById(id);
    }

    // Дополнительные методы с кешированием
    @Transactional(readOnly = true)
    @Cacheable(value = "alertsByBus", key = "#busId")
    public List<Alert> findByBusId(Long busId) {
        return alertRepository.findByBusId(busId);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "alertsByUser", key = "#userId")
    public List<Alert> findByAssignedToUserId(Long userId) {
        return alertRepository.findByAssignedToUserId(userId);
    }

    // Метод для принудительного сброса кеша
    @Caching(evict = {
        @CacheEvict(value = "alerts", allEntries = true),
        @CacheEvict(value = "alertsByStatus", allEntries = true),
        @CacheEvict(value = "alertsByBus", allEntries = true),
        @CacheEvict(value = "alertsByUser", allEntries = true)
    })
    public void clearAllCache() {
        // Метод только для очистки кеша
    }
}