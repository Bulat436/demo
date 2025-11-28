package com.example.demo.controller;

import com.example.demo.model.Alert;
import com.example.demo.model.StatusType;
import com.example.demo.service.CachedAlertService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {
    private final CachedAlertService alertService;

    public AlertController(CachedAlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MANAGER')")
    public List<Alert> getAllAlerts(@RequestParam(required = false) StatusType status) {
        if (status != null) {
            return alertService.findByStatus(status);
        }
        return alertService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<Alert> getAlertById(@PathVariable Long id) {
        return alertService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> createAlert(@Valid @RequestBody Alert alert, BindingResult result) {
        if (result.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            result.getFieldErrors().forEach(error -> 
                errors.put(error.getField(), error.getDefaultMessage()));
            return ResponseEntity.badRequest().body(errors);
        }

        Alert createdAlert = alertService.create(alert);
        return ResponseEntity.created(URI.create("/api/alerts/" + createdAlert.getId()))
                .body(createdAlert);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Alert> updateStatus(@PathVariable Long id, 
                                             @RequestParam StatusType status) {
        try {
            Alert updatedAlert = alertService.updateStatus(id, status);
            return ResponseEntity.ok(updatedAlert);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Alert> assignAlert(@PathVariable Long id, 
                                            @RequestParam Long userId) {
        try {
            Alert updatedAlert = alertService.assignToUser(id, userId);
            return ResponseEntity.ok(updatedAlert);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id) {
        try {
            alertService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/cache/clear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> clearCache() {
        alertService.clearAllCache();
        return ResponseEntity.ok("Кеш успешно очищен");
    }
}