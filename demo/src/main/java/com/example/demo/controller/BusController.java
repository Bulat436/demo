package com.example.demo.controller;

import com.example.demo.dto.BusDto;
import com.example.demo.service.BusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Slf4j
@Tag(name = "Bus", description = "API для управления автобусами")
@RestController
@RequestMapping("/api/buses")
@RequiredArgsConstructor
public class BusController {
    
    private final BusService busService;
    
    @Operation(summary = "Получить все автобусы")
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<BusDto>> getAllBuses() {
        log.debug("GET /api/buses - получение всех автобусов");
        List<BusDto> buses = busService.getAllBuses();
        log.info("Возвращено {} автобусов", buses.size());
        return ResponseEntity.ok(buses);
    }
    
    @Operation(summary = "Получить автобус по ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<BusDto> getBusById(@PathVariable Long id) {
        log.debug("GET /api/buses/{} - получение автобуса", id);
        BusDto bus = busService.getBusById(id);
        log.info("Найден автобус: id={}, модель={}", bus.id(), bus.model());
        return ResponseEntity.ok(bus);
    }
    
    @Operation(summary = "Поиск автобусов по модели")
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<BusDto>> searchBuses(@RequestParam String model) {
        log.debug("GET /api/buses/search?model={} - поиск автобусов", model);
        List<BusDto> buses = busService.searchBusesByModel(model);
        log.info("Найдено {} автобусов по модели '{}'", buses.size(), model);
        return ResponseEntity.ok(buses);
    }
    
    @Operation(summary = "Создать новый автобус")
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<BusDto> createBus(@Valid @RequestBody BusDto busDto) {
        log.debug("POST /api/buses - создание автобуса: модель={}", busDto.model());
        BusDto createdBus = busService.createBus(busDto);
        log.info("Автобус создан: id={}, модель={}", createdBus.id(), createdBus.model());
        return ResponseEntity
                .created(URI.create("/api/buses/" + createdBus.id()))
                .body(createdBus);
    }
    
    @Operation(summary = "Обновить автобус")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<BusDto> updateBus(@PathVariable Long id, 
                                           @Valid @RequestBody BusDto busDto) {
        log.debug("PUT /api/buses/{} - обновление автобуса", id);
        BusDto updatedBus = busService.updateBus(id, busDto);
        log.info("Автобус обновлен: id={}, модель={}", updatedBus.id(), updatedBus.model());
        return ResponseEntity.ok(updatedBus);
    }
    
    @Operation(summary = "Удалить автобус")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBus(@PathVariable Long id) {
        log.debug("DELETE /api/buses/{} - удаление автобуса", id);
        busService.deleteBus(id);
        log.info("Автобус удален: id={}", id);
        return ResponseEntity.noContent().build();
    }
}