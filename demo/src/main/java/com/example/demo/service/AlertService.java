package com.example.demo.service;

import com.example.demo.model.Alert;
import com.example.demo.model.StatusType;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

public interface AlertService {
    @Cacheable(value = "alerts", unless = "#result.isEmpty()")
    List<Alert> findAll();
    
    @Cacheable(value = "alertsByStatus", key = "#status.name()")
    List<Alert> findByStatus(StatusType status);
    
    Optional<Alert> findById(Long id);
    
    @Transactional
    @CacheEvict(value = {"alerts", "alertsByStatus", "alertsByBus", "alertsByUser"}, allEntries = true)
    Alert create(Alert alert);
    
    @Transactional
    @CacheEvict(value = {"alerts", "alertsByStatus", "alertsByBus", "alertsByUser"}, allEntries = true)
    Alert updateStatus(Long alertId, StatusType newStatus);
    
    @Transactional
    @CacheEvict(value = {"alerts", "alertsByStatus", "alertsByBus", "alertsByUser"}, allEntries = true)
    Alert assignToUser(Long alertId, Long userId);
    
    @Transactional
    @CacheEvict(value = {"alerts", "alertsByStatus", "alertsByBus", "alertsByUser"}, allEntries = true)
    void deleteById(Long id);
    Alert addFileToAlert(Long alertId, String filePath);
}