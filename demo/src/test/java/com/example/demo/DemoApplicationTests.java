package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create",
    "spring.sql.init.mode=never"
})
class DemoApplicationTests {

    @Test
    void contextLoads() {
    }
}