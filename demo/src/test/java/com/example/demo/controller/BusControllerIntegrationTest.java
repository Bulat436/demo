package com.example.demo.controller;

import com.example.demo.dto.BusDto;
import com.example.demo.service.BusService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BusControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BusService busService;

    @Test
    @WithMockUser(roles = {"USER"})
    void getAllBuses_ShouldReturnBuses() throws Exception {
        List<BusDto> buses = Arrays.asList(
            new BusDto(1L, "Mercedes-Benz"),
            new BusDto(2L, "Volvo")
        );
        
        when(busService.getAllBuses()).thenReturn(buses);

        mockMvc.perform(get("/api/buses")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].model", is("Mercedes-Benz")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].model", is("Volvo")));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void getBusById_WhenBusExists_ShouldReturnBus() throws Exception {
        BusDto bus = new BusDto(1L, "Mercedes-Benz");
        
        when(busService.getBusById(1L)).thenReturn(bus);

        mockMvc.perform(get("/api/buses/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.model", is("Mercedes-Benz")));
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void createBus_WithValidData_ShouldCreateBus() throws Exception {
        BusDto newBus = new BusDto(null, "New Bus Model");
        BusDto createdBus = new BusDto(1L, "New Bus Model");
        
        when(busService.createBus(any(BusDto.class))).thenReturn(createdBus);

        mockMvc.perform(post("/api/buses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newBus)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.model", is("New Bus Model")));
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void updateBus_ShouldUpdateBus() throws Exception {
        BusDto updatedBus = new BusDto(1L, "Updated Model");
        
        when(busService.updateBus(eq(1L), any(BusDto.class))).thenReturn(updatedBus);

        mockMvc.perform(put("/api/buses/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedBus)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.model", is("Updated Model")));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void deleteBus_ShouldDeleteBus() throws Exception {
        doNothing().when(busService).deleteBus(1L);

        mockMvc.perform(delete("/api/buses/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }
}