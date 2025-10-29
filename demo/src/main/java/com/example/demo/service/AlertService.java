package com.example.demo.service;

import com.example.demo.model.Alert;
import com.example.demo.model.StatusType;
import java.util.List;
import java.util.Optional;

public interface AlertService {
    // Получить все инциденты
    List<Alert> findAll();
    
    // Найти инциденты по статусу
    List<Alert> findByStatus(StatusType status);
    
    // Найти инцидент по ID
    Optional<Alert> findById(Long id);
    
    // Создать новый инцидент
    Alert create(Alert alert);
    
    // Обновить статус инцидента
    Alert updateStatus(Long alertId, StatusType newStatus);
    
    // Назначить инцидент на пользователя
    Alert assignToUser(Long alertId, Long userId);
    
    // Удалить инцидент
    void deleteById(Long id);
}