package com.example.demo.model;

import com.example.demo.enums.TokenType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Token {
    private TokenType type;
    private String tokenValue;
    private LocalDateTime expiryDate;
    private boolean revoked;
    private Long userId;
}