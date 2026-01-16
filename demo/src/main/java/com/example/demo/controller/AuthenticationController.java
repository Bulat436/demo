package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.example.demo.dto.ChangePasswordRequest;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.UserDto;
import com.example.demo.dto.UserLoggedDto;
import com.example.demo.service.UserService;
import com.example.demo.service.AuthService;
import lombok.RequiredArgsConstructor;

@Tag(name = "Authentication", description = "API для аутентификации и управления сессиями")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);
    
    private final AuthService authService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "Логин пользователя", description = "Аутентификация пользователя по логину и паролю. Возвращает JWT-токены.")    
    @ApiResponse(responseCode = "200", description = "Успешная аутентификация")
    @ApiResponse(responseCode = "400", description = "Неверные учетные данные")    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @CookieValue(name = "access_token", required = false) String accessToken,
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            @RequestBody LoginRequest loginRequest) {
        log.info("Попытка входа пользователя: {}", loginRequest.username());
        log.debug("Существующие токены - access: {}, refresh: {}", 
                 accessToken != null ? "присутствует" : "отсутствует", 
                 refreshToken != null ? "присутствует" : "отсутствует");
        
        ResponseEntity<LoginResponse> response = authService.login(loginRequest, accessToken, refreshToken);
        
        if (response.getStatusCode() == HttpStatus.OK) {
            log.info("Успешный вход пользователя: {}", loginRequest.username());
        } else {
            log.warn("Неудачная попытка входа пользователя: {}", loginRequest.username());
        }
        
        return response;
    }

    @Operation(summary = "Обновление токена", description = "Генерация нового access-токена по refresh-токену")
    @ApiResponse(responseCode = "200", description = "Токен успешно обновлен")
    @ApiResponse(responseCode = "400", description = "Недействительный refresh-токен")
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {
        log.info("Запрос на обновление токена");
        log.debug("Refresh токен предоставлен: {}", refreshToken != null ? "присутствует" : "отсутствует");
        
        if (refreshToken == null) {
            log.warn("Refresh токен отсутствует");
            return ResponseEntity.badRequest().build();
        }
        
        ResponseEntity<LoginResponse> response = authService.refresh(refreshToken);
        
        if (response.getStatusCode() == HttpStatus.OK) {
            log.info("Токен успешно обновлен");
        } else {
            log.warn("Не удалось обновить токен");
        }
        
        return response;
    }

    @Operation(summary = "Выход из системы", description = "Инвалидация текущих JWT-токенов")
    @ApiResponse(responseCode = "200", description = "Сессия завершена")
    @PostMapping("/logout")
    public ResponseEntity<LoginResponse> logout(
            @CookieValue(name = "access_token", required = false) String accessToken,
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {
        log.info("Запрос на выход из системы");
        log.debug("Токены для инвалидации - access: {}, refresh: {}", 
                 accessToken != null ? "присутствует" : "отсутствует", 
                 refreshToken != null ? "присутствует" : "отсутствует");
        
        ResponseEntity<LoginResponse> response = authService.logout(accessToken, refreshToken);
        log.info("Пользователь успешно вышел из системы");
        
        return response;
    }

    @Operation(summary = "Информация о пользователе", description = "Получение данных текущего аутентифицированного пользователя")
    @ApiResponse(responseCode = "200", description = "Данные пользователя")
    @ApiResponse(responseCode = "401", description = "Требуется аутентификация")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/info")
    public ResponseEntity<UserLoggedDto> userLoggedInfo() {
        log.debug("Запрос информации о пользователе");
        try {
            UserLoggedDto userInfo = authService.getUserLoggedInfo();
            log.info("Информация о пользователе получена: {}", userInfo.username());
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            log.error("Ошибка получения информации о пользователе: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Operation(summary = "Смена пароля", description = "Изменение пароля текущего пользователя")
    @PutMapping("/change_password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        log.info("Запрос на смену пароля");
        
        if (!request.confirmPassword().equals(request.newPassword())) {
            log.warn("Смена пароля не удалась: пароли не совпадают");
            return ResponseEntity.badRequest().body("Пароли не совпадают");
        }
        
        try {
            UserDto user = userService.getUserByUsername(authService.getUserLoggedInfo().username());
            if (user == null) {
                log.warn("Смена пароля не удалась: пользователь не найден");
                return ResponseEntity.badRequest().body("Пользователь не найден");
            }
            
            if (passwordEncoder.matches(request.currentPassword(), user.password())) {
                userService.updateUser(user.id(),
                        new UserDto(user.id(), user.username(),
                                request.newPassword(), user.role(), user.permissions()));
                log.info("Пароль успешно изменен для пользователя: {}", user.username());
                return ResponseEntity.ok("Пароль успешно изменен");
            }
            
            log.warn("Смена пароля не удалась: неверный текущий пароль для пользователя: {}", user.username());
            return ResponseEntity.badRequest().body("Текущий пароль неверен");
            
        } catch (Exception e) {
            log.error("Ошибка при смене пароля: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Ошибка при смене пароля: " + e.getMessage());
        }
    }
}