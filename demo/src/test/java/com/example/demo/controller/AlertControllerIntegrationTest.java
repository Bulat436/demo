package com.example.demo.controller;

import com.example.demo.model.Alert;
import com.example.demo.model.EventType;
import com.example.demo.model.StatusType;
import com.example.demo.service.CachedAlertService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AlertControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private CachedAlertService alertService;

    private Alert testAlert;

    @BeforeEach
    void setUp() {

        testAlert = new Alert();
        testAlert.setId(1L);
        testAlert.setBusId(101L);
        testAlert.setType(EventType.ACCIDENT);
        testAlert.setTimestamp(LocalDateTime.now());
        testAlert.setLocation("Москва, Ленинский проспект");
        testAlert.setDescription("Столкновение");
        testAlert.setStatus(StatusType.NEW);
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void getAllAlerts_ShouldReturnAlerts() throws Exception {
        List<Alert> alerts = Arrays.asList(testAlert);
        when(alertService.findAll()).thenReturn(alerts);

        mockMvc.perform(get("/api/alerts")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].busId", is(101)))
                .andExpect(jsonPath("$[0].type", is("ACCIDENT")));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void getAllAlerts_WithStatusFilter_ShouldReturnFilteredAlerts() throws Exception {
        List<Alert> alerts = Arrays.asList(testAlert);
        when(alertService.findByStatus(StatusType.NEW)).thenReturn(alerts);

        mockMvc.perform(get("/api/alerts")
                .param("status", "NEW")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("NEW")));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void getAlertById_WhenAlertExists_ShouldReturnAlert() throws Exception {
        when(alertService.findById(1L)).thenReturn(Optional.of(testAlert));

        mockMvc.perform(get("/api/alerts/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.busId", is(101)));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void getAlertById_WhenAlertNotExists_ShouldReturn404() throws Exception {
        when(alertService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/alerts/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void createAlert_WithValidData_ShouldCreateAlert() throws Exception {
        Alert newAlert = new Alert();
        newAlert.setBusId(102L);
        newAlert.setType(EventType.HARD_BRAKING);
        newAlert.setLocation("Санкт-Петербург");
        newAlert.setDescription("Резкое торможение");

        when(alertService.create(any(Alert.class))).thenReturn(testAlert);

        mockMvc.perform(post("/api/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newAlert)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void createAlert_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        Alert invalidAlert = new Alert(); 

        mockMvc.perform(post("/api/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidAlert)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$", aMapWithSize(greaterThan(0))));
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void updateStatus_ShouldUpdateStatus() throws Exception {
        when(alertService.updateStatus(1L, StatusType.IN_PROGRESS)).thenReturn(testAlert);

        mockMvc.perform(put("/api/alerts/1/status")
                .param("status", "IN_PROGRESS")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void assignAlert_ShouldAssignToUser() throws Exception {
        when(alertService.assignToUser(1L, 5L)).thenReturn(testAlert);

        mockMvc.perform(put("/api/alerts/1/assign")
                .param("userId", "5")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void deleteAlert_ShouldDeleteAlert() throws Exception {
        doNothing().when(alertService).deleteById(1L);

        mockMvc.perform(delete("/api/alerts/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void clearCache_ShouldClearCache() throws Exception {
        // Mock метод clearAllCache
        doNothing().when(alertService).clearAllCache();

        // Act & Assert
        mockMvc.perform(post("/api/alerts/cache/clear")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Кеш успешно очищен"));
    }
}