package com.example.demo.controller;

import com.example.demo.dto.ChangePasswordRequest;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.UserDto;
import com.example.demo.dto.UserLoggedDto;
import com.example.demo.service.AuthService;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    
    private final AuthService authService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @CookieValue(name = "access_token", required = false) String accessToken,
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            @RequestBody LoginRequest loginRequest) {
        return authService.login(loginRequest, accessToken, refreshToken);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {
        if (refreshToken == null) {
            return ResponseEntity.notFound().build();
        }
        return authService.refresh(refreshToken);
    }

    @PostMapping("/logout")
    public ResponseEntity<LoginResponse> logout(
            @CookieValue(name = "access_token", required = false) String accessToken,
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {
        return authService.logout(accessToken, refreshToken);
    }

    @GetMapping("/info")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserLoggedDto> userLoggedInfo() {
        return ResponseEntity.ok(authService.getUserLoggedInfo());
    }

    @PutMapping("/change_password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        if (!request.confirmPassword().equals(request.newPassword())) {
            return ResponseEntity.badRequest().body("Пароли не совпадают");
        }
        
        UserDto user = userService.getUserByUsername(authService.getUserLoggedInfo().username());
        if (passwordEncoder.matches(request.currentPassword(), user.password())) {
            userService.updateUser(user.id(),
                    new UserDto(user.id(), user.username(),
                            request.newPassword(), user.role(), user.permissions()));
            return ResponseEntity.ok("Пароль успешно изменен");
        }
        return ResponseEntity.badRequest().body("Текущий пароль неверен");
    }
}