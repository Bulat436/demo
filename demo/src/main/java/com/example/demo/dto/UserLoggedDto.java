package com.example.demo.dto;

import java.util.Set;

public record UserLoggedDto(String username, String role, Set<String> permissions) {
    
}