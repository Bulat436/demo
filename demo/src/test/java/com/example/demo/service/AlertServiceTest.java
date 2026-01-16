package com.example.demo.service;

import com.example.demo.exception.AlertNotFoundException;
import com.example.demo.model.Alert;
import com.example.demo.model.EventType;
import com.example.demo.model.StatusType;
import com.example.demo.repository.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @InjectMocks
    private CachedAlertService alertService;

    private Alert testAlert;
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        testAlert = new Alert();
        testAlert.setId(1L);
        testAlert.setBusId(101L);
        testAlert.setType(EventType.ACCIDENT);
        testAlert.setLocation("Москва, Ленинский проспект");
        testAlert.setDescription("Столкновение с другим автомобилем");
        testAlert.setStatus(StatusType.NEW);
        testAlert.setTimestamp(LocalDateTime.now());
        
        cacheManager = new ConcurrentMapCacheManager("alerts", "alertsByStatus", "alertsByBus", "alertsByUser");
    }

    @Test
    void findAll_ShouldReturnAllAlerts() {
        List<Alert> alerts = Arrays.asList(testAlert, testAlert);
        when(alertRepository.findAll()).thenReturn(alerts);

        List<Alert> result = alertService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getBusId()).isEqualTo(101L);
        verify(alertRepository, times(1)).findAll();
    }

    @Test
    void findById_WhenAlertExists_ShouldReturnAlert() {
        when(alertRepository.findById(1L)).thenReturn(Optional.of(testAlert));

        Optional<Alert> result = alertService.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getStatus()).isEqualTo(StatusType.NEW);
    }

    @Test
    void findById_WhenAlertNotExists_ShouldReturnEmpty() {
        when(alertRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Alert> result = alertService.findById(999L);

        assertThat(result).isEmpty();
    }

    @Test
    void findByStatus_ShouldReturnAlertsWithStatus() {
        List<Alert> alerts = Arrays.asList(testAlert);
        when(alertRepository.findByStatus(StatusType.NEW)).thenReturn(alerts);

        List<Alert> result = alertService.findByStatus(StatusType.NEW);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(StatusType.NEW);
    }

    @Test
    void create_ShouldSaveAndReturnAlert() {
        Alert newAlert = new Alert();
        newAlert.setBusId(102L);
        newAlert.setType(EventType.HARD_BRAKING);
        newAlert.setLocation("Санкт-Петербург, Невский проспект");
        newAlert.setDescription("Резкое торможение");
        
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert alert = invocation.getArgument(0);
            alert.setId(2L);
            alert.setTimestamp(LocalDateTime.now());
            alert.setStatus(StatusType.NEW);
            return alert;
        });

        Alert result = alertService.create(newAlert);

        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getStatus()).isEqualTo(StatusType.NEW);
        assertThat(result.getTimestamp()).isNotNull();
        verify(alertRepository, times(1)).save(any(Alert.class));
    }

    @Test
    void updateStatus_WhenAlertExists_ShouldUpdateStatus() {
        Alert existingAlert = new Alert();
        existingAlert.setId(1L);
        existingAlert.setStatus(StatusType.NEW);
        
        when(alertRepository.findById(1L)).thenReturn(Optional.of(existingAlert));
        when(alertRepository.save(any(Alert.class))).thenReturn(existingAlert);

        Alert result = alertService.updateStatus(1L, StatusType.IN_PROGRESS);

        assertThat(result.getStatus()).isEqualTo(StatusType.IN_PROGRESS);
        verify(alertRepository, times(1)).findById(1L);
        verify(alertRepository, times(1)).save(existingAlert);
    }

    @Test
    void updateStatus_WhenAlertNotExists_ShouldThrowException() {
        when(alertRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.updateStatus(999L, StatusType.RESOLVED))
                .isInstanceOf(AlertNotFoundException.class)
                .hasMessageContaining("Инцидент с ID 999 не найден");
        
        verify(alertRepository, never()).save(any());
    }

    @Test
    void assignToUser_ShouldAssignUserAndChangeStatus() {
        Alert alert = new Alert();
        alert.setId(1L);
        alert.setStatus(StatusType.NEW);
        
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(Alert.class))).thenReturn(alert);

        Alert result = alertService.assignToUser(1L, 5L);

        assertThat(result.getAssignedToUserId()).isEqualTo(5L);
        assertThat(result.getStatus()).isEqualTo(StatusType.IN_PROGRESS);
        verify(alertRepository, times(1)).save(alert);
    }

    @Test
    void deleteById_WhenAlertExists_ShouldDelete() {
        when(alertRepository.existsById(1L)).thenReturn(true);
        doNothing().when(alertRepository).deleteById(1L);

        alertService.deleteById(1L);

        verify(alertRepository, times(1)).deleteById(1L);
        verify(alertRepository, times(1)).existsById(1L);
    }

    @Test
    void deleteById_WhenAlertNotExists_ShouldThrowException() {
        when(alertRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> alertService.deleteById(999L))
                .isInstanceOf(AlertNotFoundException.class);
        
        verify(alertRepository, never()).deleteById(anyLong());
    }

    @Test
    void findByBusId_ShouldReturnAlertsForBus() {
        List<Alert> alerts = Arrays.asList(testAlert);
        when(alertRepository.findByBusId(101L)).thenReturn(alerts);

        List<Alert> result = alertService.findByBusId(101L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBusId()).isEqualTo(101L);
    }

    @Test
    void findByAssignedToUserId_ShouldReturnAlertsForUser() {
        testAlert.setAssignedToUserId(5L);
        List<Alert> alerts = Arrays.asList(testAlert);
        when(alertRepository.findByAssignedToUserId(5L)).thenReturn(alerts);

        List<Alert> result = alertService.findByAssignedToUserId(5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAssignedToUserId()).isEqualTo(5L);
    }
}