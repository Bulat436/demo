package com.example.demo.controller;

import com.example.demo.dto.ChangePasswordRequest;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.UserLoggedDto;
import com.example.demo.service.AuthService;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder; 

    @MockBean
    private AuthService authService;

    @MockBean
    private UserService userService;

    @Test
    void login_WithValidCredentials_ShouldReturnSuccess() throws Exception {
        LoginRequest loginRequest = new LoginRequest("testuser", "password");
        LoginResponse loginResponse = new LoginResponse(true, "ROLE_USER");
        
        when(authService.login(any(), any(), any()))
                .thenReturn(ResponseEntity.ok(loginResponse));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isLogged").value(true))
                .andExpect(jsonPath("$.roles").value("ROLE_USER"));
    }

    @Test
    void login_WithInvalidCredentials_ShouldReturnError() throws Exception {
        LoginRequest loginRequest = new LoginRequest("wronguser", "wrongpass");
        
        when(authService.login(any(), any(), any()))
                .thenReturn(ResponseEntity.badRequest().body(new LoginResponse(false, "")));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isLogged").value(false));
    }

    @Test
    void refresh_WithValidToken_ShouldReturnNewToken() throws Exception {
        LoginResponse loginResponse = new LoginResponse(true, "ROLE_USER");
        
        when(authService.refresh(anyString()))
                .thenReturn(ResponseEntity.ok(loginResponse));

        mockMvc.perform(post("/api/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie("refresh_token", "valid-token"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser")
    void userLoggedInfo_WhenAuthenticated_ShouldReturnUserInfo() throws Exception {
        UserLoggedDto userInfo = new UserLoggedDto("testuser", "ROLE_USER", Set.of());
        
        when(authService.getUserLoggedInfo()).thenReturn(userInfo);

        mockMvc.perform(get("/api/auth/info")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void changePassword_WithValidData_ShouldChangePassword() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "oldPassword",
                "newPassword123",
                "newPassword123"
        );
        
        UserLoggedDto userInfo = new UserLoggedDto("testuser", "ROLE_USER", Set.of());
        
        when(authService.getUserLoggedInfo()).thenReturn(userInfo);

        String encodedOldPassword = passwordEncoder.encode("oldPassword");
        when(userService.getUserByUsername("testuser"))
                .thenReturn(new com.example.demo.dto.UserDto(1L, "testuser", encodedOldPassword, "ROLE_USER", Set.of()));

        when(userService.updateUser(eq(1L), any())).thenReturn(
                new com.example.demo.dto.UserDto(1L, "testuser", "encodedNewPassword", "ROLE_USER", Set.of())
        );

        mockMvc.perform(put("/api/auth/change_password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Пароль успешно изменен"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void changePassword_WhenPasswordsDontMatch_ShouldReturnError() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "oldPassword",
                "newPassword123",
                "differentPassword"
        );

        mockMvc.perform(put("/api/auth/change_password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Пароли не совпадают"));
    }

    @Test
    void logout_ShouldClearSession() throws Exception {
        LoginResponse logoutResponse = new LoginResponse(false, "");
        
        when(authService.logout(any(), any()))
                .thenReturn(ResponseEntity.ok(logoutResponse));

        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isLogged").value(false));
    }
}