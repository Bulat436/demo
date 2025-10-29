package com.example.demo.controller;

import com.example.demo.model.Alert;
import com.example.demo.model.StatusType;
import com.example.demo.service.AlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController // Говорим Spring, что это REST контроллер
@RequestMapping("/api/alerts") // Все методы будут начинаться с /api/alerts
public class AlertController {

    private final AlertService alertService;

    // Spring автоматически передаст сервис
    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    // GET /api/alerts - получить все инциденты
    @GetMapping
    public List<Alert> getAllAlerts(@RequestParam(required = false) StatusType status) {
        if (status != null) {
            return alertService.findByStatus(status); // Фильтрация по статусу
        }
        return alertService.findAll(); // Все инциденты
    }

    // GET /api/alerts/{id} - получить инцидент по ID
    @GetMapping("/{id}")
    public ResponseEntity<Alert> getAlertById(@PathVariable Long id) {
        return alertService.findById(id)
                .map(ResponseEntity::ok) // Если найден - возвращаем 200 OK
                .orElse(ResponseEntity.notFound().build()); // Если нет - 404 Not Found
    }

    // POST /api/alerts - создать новый инцидент
    @PostMapping
    public ResponseEntity<Alert> createAlert(@RequestBody Alert alert) {
        Alert createdAlert = alertService.create(alert);
        // Возвращаем статус 201 Created и ссылку на новый ресурс
        return ResponseEntity.created(URI.create("/api/alerts/" + createdAlert.getId()))
                .body(createdAlert);
    }

    // PUT /api/alerts/{id}/status - изменить статус инцидента
    @PutMapping("/{id}/status")
    public ResponseEntity<Alert> updateStatus(@PathVariable Long id, 
                                             @RequestParam StatusType status) {
        try {
            Alert updatedAlert = alertService.updateStatus(id, status);
            return ResponseEntity.ok(updatedAlert);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // PUT /api/alerts/{id}/assign - назначить инцидент на пользователя
    @PutMapping("/{id}/assign")
    public ResponseEntity<Alert> assignAlert(@PathVariable Long id, 
                                            @RequestParam Long userId) {
        try {
            Alert updatedAlert = alertService.assignToUser(id, userId);
            return ResponseEntity.ok(updatedAlert);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE /api/alerts/{id} - удалить инцидент
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id) {
        alertService.deleteById(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}