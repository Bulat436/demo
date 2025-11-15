package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class TransactionConfig {
    // Transaction management is enabled by Spring Boot automatically
    // This configuration class explicitly enables it for clarity
}