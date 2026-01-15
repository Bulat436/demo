package com.example.demo.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ChangePasswordRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void whenAllFieldsValid_thenNoViolations() {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "currentPass123",
                "newPassword123",
                "newPassword123"
        );

        Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void whenCurrentPasswordBlank_thenViolation() {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "",
                "newPassword123",
                "newPassword123"
        );

        Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Текущий пароль не может быть пустым");
    }

    @Test
    void whenNewPasswordTooShort_thenViolation() {

        ChangePasswordRequest request = new ChangePasswordRequest(
                "currentPass123",
                "short",
                "short"
        );

        Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Новый пароль должен содержать минимум 8 символов");
    }

    @Test
    void whenConfirmPasswordBlank_thenViolation() {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "currentPass123",
                "newPassword123",
                ""
        );

        Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Подтверждение пароля не может быть пустым");
    }


    @Test
    void whenNewPasswordBlank_thenViolation() {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "currentPass123",
                "",
                "newPassword123"
        );

        Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(2);
        List<String> messages = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());
        assertThat(messages).contains("Новый пароль не может быть пустым");
        assertThat(messages).contains("Новый пароль должен содержать минимум 8 символов");
    }

    @Test
    void whenAllFieldsBlank_thenMultipleViolations() {
        ChangePasswordRequest request = new ChangePasswordRequest("", "", "");

        Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(4);
    }


}