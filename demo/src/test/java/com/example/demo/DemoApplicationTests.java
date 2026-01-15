package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DemoApplicationTests {

    @Test
    void contextLoads() {
        // Тест проверяет, что контекст Spring Boot успешно загружается
    }

    @Test
    void testApplicationStarts() {
        assertThat(true).isTrue();
    }
}