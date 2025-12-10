package com.example.demo.service;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.UserLoggedDto;
import org.springframework.http.ResponseEntity;

public interface AuthService {
    ResponseEntity<LoginResponse> login(LoginRequest loginRequest, String accessToken, String refreshToken);
    ResponseEntity<LoginResponse> refresh(String refreshToken);
    ResponseEntity<LoginResponse> logout(String accessToken, String refreshToken);
    UserLoggedDto getUserLoggedInfo();
}