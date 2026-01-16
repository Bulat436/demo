package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

public record BusDto(
    Long id,
    
    @NotBlank(message = "Модель автобуса не может быть пустой")
    String model
) {}