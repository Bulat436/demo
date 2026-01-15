package com.example.demo.controller;

import com.example.demo.model.Alert;
import com.example.demo.model.StatusType;
import com.example.demo.service.CachedAlertService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {
    private static final Logger log = LoggerFactory.getLogger(AlertController.class);

    private final CachedAlertService alertService;

    public AlertController(CachedAlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MANAGER')")
    public List<Alert> getAllAlerts(@RequestParam(required = false) StatusType status) {
        log.debug("Получение всех инцидентов, фильтр по статусу: {}", status != null ? status : "нет");
        
        List<Alert> alerts;
        if (status != null) {
            alerts = alertService.findByStatus(status);
            log.info("Получено {} инцидентов со статусом: {}", alerts.size(), status);
        } else {
            alerts = alertService.findAll();
            log.info("Получено {} инцидентов", alerts.size());
        }
        
        return alerts;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<Alert> getAlertById(@PathVariable Long id) {
        log.debug("Получение инцидента по ID: {}", id);
        
        Optional<Alert> alert = alertService.findById(id);
        if (alert.isPresent()) {
            log.info("Инцидент найден: id={}, тип={}, статус={}", 
                    id, alert.get().getType(), alert.get().getStatus());
            return ResponseEntity.ok(alert.get());
        } else {
            log.warn("Инцидент не найден: id={}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/bus/{busId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MANAGER')")
    public List<Alert> getAlertsByBus(@PathVariable Long busId) {
        return alertService.findByBusId(busId); // Метод есть в CachedAlertService
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MANAGER')")
    public List<Alert> getAlertsByUser(@PathVariable Long userId) {
        return alertService.findByAssignedToUserId(userId);
    }

    @GetMapping("/cache-test")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<String> cacheTest() {
        log.info("=== БЫСТРЫЙ ТЕСТ КЭША ===");
        
        StringBuilder result = new StringBuilder();
        
        // Тест 1
        result.append("Тест 1 (findAll): ");
        long start1 = System.currentTimeMillis();
        alertService.findAll();
        long time1 = System.currentTimeMillis() - start1;
        result.append(time1).append("ms\n");
        
        // Тест 2 - должен быть быстрее
        result.append("Тест 2 (findAll from cache): ");
        long start2 = System.currentTimeMillis();
        alertService.findAll();
        long time2 = System.currentTimeMillis() - start2;
        result.append(time2).append("ms\n");
        
        // Тест 3 - UserDetails кэш
        result.append("Тест 3 (UserDetails cache): \n");
        log.info("Результаты теста кэша:\n{}", result);
        
        return ResponseEntity.ok(result.toString());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> createAlert(@Valid @RequestBody Alert alert, BindingResult result) {
        log.debug("Создание нового инцидента для автобуса: {}, тип: {}", alert.getBusId(), alert.getType());
        
        if (result.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            result.getFieldErrors().forEach(error -> 
                errors.put(error.getField(), error.getDefaultMessage()));
            
            log.warn("Создание инцидента не удалось - ошибки валидации: {}", errors);
            return ResponseEntity.badRequest().body(errors);
        }
        
        try {
            Alert createdAlert = alertService.create(alert);
            log.info("Инцидент успешно создан: id={}, автобус={}, тип={}", 
                    createdAlert.getId(), createdAlert.getBusId(), createdAlert.getType());
            
            return ResponseEntity.created(URI.create("/api/alerts/" + createdAlert.getId()))
                    .body(createdAlert);
        } catch (Exception e) {
            log.error("Ошибка создания инцидента: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Ошибка создания инцидента: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Alert> updateStatus(@PathVariable Long id, 
                                             @RequestParam StatusType status) {
        log.debug("Обновление статуса инцидента: id={}, новый статус={}", id, status);
        
        try {
            Alert updatedAlert = alertService.updateStatus(id, status);
            log.info("Статус инцидента обновлен: id={}, старый статус={}, новый статус={}", 
                    id, updatedAlert.getStatus(), status);
            return ResponseEntity.ok(updatedAlert);
        } catch (RuntimeException e) {
            log.error("Ошибка обновления статуса инцидента: id={}, ошибка={}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Alert> assignAlert(@PathVariable Long id, 
                                            @RequestParam Long userId) {
        log.debug("Назначение инцидента: инцидентId={}, пользовательId={}", id, userId);
        
        try {
            Alert updatedAlert = alertService.assignToUser(id, userId);
            log.info("Инцидент назначен: инцидентId={}, пользовательId={}", id, userId);
            return ResponseEntity.ok(updatedAlert);
        } catch (RuntimeException e) {
            log.error("Ошибка назначения инцидента: инцидентId={}, пользовательId={}, ошибка={}", 
                     id, userId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id) {
        log.debug("Удаление инцидента: id={}", id);
        
        try {
            alertService.deleteById(id);
            log.info("Инцидент удален: id={}", id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Ошибка удаления инцидента: id={}, ошибка={}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/cache/clear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> clearCache() {
        log.info("Запрос на очистку кэша");
        
        try {
            alertService.clearAllCache();
            log.info("Кэш успешно очищен");
            return ResponseEntity.ok("Кеш успешно очищен");
        } catch (Exception e) {
            log.error("Ошибка очистки кэша: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Ошибка очистки кеша: " + e.getMessage());
        }
    }
}