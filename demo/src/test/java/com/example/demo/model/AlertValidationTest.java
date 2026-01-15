package com.example.demo.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AlertValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void whenAllFieldsValid_thenNoViolations() {
        Alert alert = new Alert();
        alert.setBusId(101L);
        alert.setType(EventType.ACCIDENT);
        alert.setLocation("Москва, Ленинский проспект");
        alert.setDescription("Столкновение с другим автомобилем");

        Set<ConstraintViolation<Alert>> violations = validator.validate(alert);

        assertThat(violations).isEmpty();
    }

    @Test
    void whenBusIdNull_thenViolation() {
        Alert alert = new Alert();
        alert.setBusId(null); 
        alert.setType(EventType.ACCIDENT);
        alert.setLocation("Москва");
        alert.setDescription("Описание");

        Set<ConstraintViolation<Alert>> violations = validator.validate(alert);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Bus ID не может быть пустым");
    }

    @Test
    void whenTypeNull_thenViolation() {
        Alert alert = new Alert();
        alert.setBusId(101L);
        alert.setType(null); 
        alert.setLocation("Москва");
        alert.setDescription("Описание");

        Set<ConstraintViolation<Alert>> violations = validator.validate(alert);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Тип инцидента обязателен");
    }

    @Test
    void whenLocationBlank_thenViolation() {
        Alert alert = new Alert();
        alert.setBusId(101L);
        alert.setType(EventType.ACCIDENT);
        alert.setLocation(""); 
        alert.setDescription("Описание");

        Set<ConstraintViolation<Alert>> violations = validator.validate(alert);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Местоположение не может быть пустым");
    }

    @Test
    void whenDescriptionBlank_thenViolation() {
        Alert alert = new Alert();
        alert.setBusId(101L);
        alert.setType(EventType.ACCIDENT);
        alert.setLocation("Москва");
        alert.setDescription("");

        Set<ConstraintViolation<Alert>> violations = validator.validate(alert);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Описание не может быть пустым");
    }

    @Test
    void whenAllRequiredFieldsMissing_thenMultipleViolations() {
        Alert alert = new Alert(); 

        Set<ConstraintViolation<Alert>> violations = validator.validate(alert);

        assertThat(violations).hasSize(4); 
    }

    @Test
    void whenPrePersist_thenDefaultValuesSet() {
        Alert alert = new Alert();
        alert.setBusId(101L);
        alert.setType(EventType.ACCIDENT);
        alert.setLocation("Москва");
        alert.setDescription("Описание");
        alert.onCreate();

        assertThat(alert.getTimestamp()).isNotNull();
        assertThat(alert.getStatus()).isEqualTo(StatusType.NEW);
    }
}