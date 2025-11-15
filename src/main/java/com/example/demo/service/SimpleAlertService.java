package com.example.demo.service;

import com.example.demo.exception.AlertNotFoundException;
import com.example.demo.model.Alert;
import com.example.demo.model.StatusType;
import com.example.demo.repository.AlertRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class SimpleAlertService implements AlertService {

    private final AlertRepository alertRepository;

    public SimpleAlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @Override
    public List<Alert> findAll() {
        return alertRepository.findAll();
    }

    @Override
    public List<Alert> findByStatus(StatusType status) {
        return alertRepository.findByStatus(status);
    }

    @Override
    public Optional<Alert> findById(Long id) {
        return alertRepository.findById(id);
    }

    @Override
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
    public Alert updateStatus(Long alertId, StatusType newStatus) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new AlertNotFoundException(alertId));
        alert.setStatus(newStatus);
        return alertRepository.save(alert);
    }

    @Override
    public Alert assignToUser(Long alertId, Long userId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new AlertNotFoundException(alertId));
        alert.setAssignedToUserId(userId);
        alert.setStatus(StatusType.IN_PROGRESS);
        return alertRepository.save(alert);
    }

    @Override
    public void deleteById(Long id) {
        alertRepository.deleteById(id);
    }
}