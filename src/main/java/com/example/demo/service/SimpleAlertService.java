package com.example.demo.service;

import com.example.demo.model.Alert;
import com.example.demo.model.StatusType;
import com.example.demo.repository.InMemoryAlertRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SimpleAlertService implements AlertService {

    private final InMemoryAlertRepository repository;

    public SimpleAlertService(InMemoryAlertRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Alert> findAll() {
        return repository.findAll();
    }

    @Override
    public List<Alert> findByStatus(StatusType status) {
        return repository.findByStatus(status);
    }

    @Override
    public Optional<Alert> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Alert create(Alert alert) {
        // Здесь можно добавить проверки перед сохранением
        return repository.save(alert);
    }

    @Override
    public Alert updateStatus(Long alertId, StatusType newStatus) {
        Optional<Alert> optionalAlert = repository.findById(alertId);
        if (optionalAlert.isPresent()) {
            Alert alert = optionalAlert.get();
            alert.setStatus(newStatus);
            return repository.save(alert);
        }
        throw new RuntimeException("Inцидент с ID " + alertId + " не найден");
    }

    @Override
    public Alert assignToUser(Long alertId, Long userId) {
        Optional<Alert> optionalAlert = repository.findById(alertId);
        if (optionalAlert.isPresent()) {
            Alert alert = optionalAlert.get();
            alert.setAssignedToUserId(userId);
            alert.setStatus(StatusType.IN_PROGRESS);
            return repository.save(alert);
        }
        throw new RuntimeException("Inцидент с ID " + alertId + " не найден");
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}