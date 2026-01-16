package com.example.demo.service;

import com.example.demo.dto.BusDto;
import com.example.demo.exception.BusNotFoundException;
import com.example.demo.mapper.BusMapper;
import com.example.demo.model.Bus;
import com.example.demo.repository.BusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BusService {
    
    private final BusRepository busRepository;
    
    @Cacheable(value = "buses")
    @Transactional(readOnly = true)
    public List<BusDto> getAllBuses() {
        log.debug("Получение всех автобусов");
        List<Bus> buses = busRepository.findAll();
        log.info("Найдено {} автобусов", buses.size());
        return buses.stream().map(BusMapper::toDto).toList();
    }
    
    @Cacheable(value = "bus", key = "#id")
    @Transactional(readOnly = true)
    public BusDto getBusById(Long id) {
        log.debug("Получение автобуса по ID: {}", id);
        Bus bus = busRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Автобус не найден: id={}", id);
                    return new BusNotFoundException(id);
                });
        return BusMapper.toDto(bus);
    }
    
    @Transactional(readOnly = true)
    public List<BusDto> searchBusesByModel(String model) {
        log.debug("Поиск автобусов по модели: {}", model);
        List<Bus> buses = busRepository.findByModelContainingIgnoreCase(model);
        log.info("Найдено {} автобусов по модели: {}", buses.size(), model);
        return buses.stream().map(BusMapper::toDto).toList();
    }
    
    @Caching(evict = {
        @CacheEvict(value = "buses", allEntries = true),
        @CacheEvict(value = "bus", key = "#result.id()")
    })
    public BusDto createBus(BusDto busDto) {
        log.info("Создание нового автобуса: модель={}", busDto.model());
        
        Bus bus = new Bus();
        bus.setModel(busDto.model());
        
        Bus savedBus = busRepository.save(bus);
        log.info("Автобус успешно создан: id={}, модель={}", 
                savedBus.getId(), savedBus.getModel());
        
        return BusMapper.toDto(savedBus);
    }
    
    @Caching(evict = {
        @CacheEvict(value = "buses", allEntries = true),
        @CacheEvict(value = "bus", key = "#id")
    })
    public BusDto updateBus(Long id, BusDto busDto) {
        log.info("Обновление автобуса: id={}", id);
        
        Bus bus = busRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Автобус не найден для обновления: id={}", id);
                    return new BusNotFoundException(id);
                });
        
        bus.setModel(busDto.model());
        
        Bus updatedBus = busRepository.save(bus);
        log.info("Автобус успешно обновлен: id={}, модель={}", 
                updatedBus.getId(), updatedBus.getModel());
        
        return BusMapper.toDto(updatedBus);
    }
    
    @Caching(evict = {
        @CacheEvict(value = "buses", allEntries = true),
        @CacheEvict(value = "bus", key = "#id")
    })
    public void deleteBus(Long id) {
        log.info("Удаление автобуса: id={}", id);
        
        if (!busRepository.existsById(id)) {
            log.warn("Автобус не найден для удаления: id={}", id);
            throw new BusNotFoundException(id);
        }
        
        busRepository.deleteById(id);
        log.info("Автобус успешно удален: id={}", id);
    }
    
    @Transactional(readOnly = true)
    public boolean busExists(String model) {
        log.debug("Проверка существования автобуса: модель={}", model);
        return busRepository.existsByModel(model);
    }
}