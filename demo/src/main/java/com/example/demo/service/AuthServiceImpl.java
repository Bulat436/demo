package com.example.demo.service;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.UserLoggedDto;
import com.example.demo.jwt.JwtTokenProvider;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public ResponseEntity<LoginResponse> login(LoginRequest loginRequest, String accessToken, String refreshToken) {
        log.info("Обработка входа пользователя: {}", loginRequest.username());
        
        try {
            // Аутентификация
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.username(),
                    loginRequest.password()
                )
            );

            // Получаем пользователя
            User user = (User) authentication.getPrincipal();
            log.debug("Пользователь аутентифицирован: {}, роль: {}", user.getUsername(), user.getRole().getAuthority());

            // Создаем claims
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", user.getRole().getAuthority());
            claims.put("username", user.getUsername());

            // Генерируем токены
            var accessTokenObj = jwtTokenProvider.generateAccessToken(
                claims, 15, ChronoUnit.MINUTES, user
            );
            var refreshTokenObj = jwtTokenProvider.generateRefreshToken(
                7, ChronoUnit.DAYS, user
            );

            // Устанавливаем cookies
            log.debug("Токены сгенерированы - access истекает: {}, refresh истекает: {}", 
                     accessTokenObj.getExpiryDate(), refreshTokenObj.getExpiryDate());
            

            ResponseCookie accessCookie = ResponseCookie.from("access_token", accessTokenObj.getTokenValue())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(15 * 60)
                .build();

            ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshTokenObj.getTokenValue())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .build();

            // Устанавливаем аутентификацию в контекст
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.info("Вход успешен для пользователя: {}", user.getUsername());
            
            return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new LoginResponse(true, user.getRole().getAuthority()));

        } catch (BadCredentialsException e) {
            log.warn("Неверные учетные данные для пользователя: {}", loginRequest.username());
            return ResponseEntity.badRequest()
                .body(new LoginResponse(false, ""));
        } catch (Exception e) {
            log.error("Ошибка входа для пользователя: {}, ошибка: {}", loginRequest.username(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(new LoginResponse(false, ""));
        }
    }

    @Override
    public ResponseEntity<LoginResponse> refresh(String refreshToken) {
        log.debug("Обработка обновления токена");
        
        try {
            if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
                log.warn("Неверный или отсутствующий refresh токен");
                return ResponseEntity.badRequest().body(new LoginResponse(false, ""));
            }

            String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
            log.debug("Refresh токен валидирован для пользователя: {}", username);
            
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("Пользователь не найден для refresh токена: {}", username);
                    return new RuntimeException("Пользователь не найден");
                });

            // Создаем claims для нового access token
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", user.getRole().getAuthority());
            claims.put("username", user.getUsername());

            var newAccessToken = jwtTokenProvider.generateAccessToken(
                claims, 15, ChronoUnit.MINUTES, user
            );

            log.debug("Новый access токен сгенерирован для пользователя: {}", username);
            
            ResponseCookie accessCookie = ResponseCookie.from("access_token", newAccessToken.getTokenValue())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(15 * 60)
                .build();

            log.info("Токен успешно обновлен для пользователя: {}", username);
                
            return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .body(new LoginResponse(true, user.getRole().getAuthority()));

        } catch (Exception e) {
            log.error("Ошибка обновления токена: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new LoginResponse(false, ""));
        }
    }

    @Override
    public ResponseEntity<LoginResponse> logout(String accessToken, String refreshToken) {
        log.info("Обработка выхода из системы");
        
        ResponseCookie accessCookie = ResponseCookie.from("access_token", "")
            .httpOnly(true)
            .secure(false)
            .path("/")
            .maxAge(0)
            .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "")
            .httpOnly(true)
            .secure(false)
            .path("/")
            .maxAge(0)
            .build();

        SecurityContextHolder.clearContext();

        log.info("Выход из системы успешен");
        
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
            .body(new LoginResponse(false, ""));
    }

    @Override
    public UserLoggedDto getUserLoggedInfo() {
        log.debug("Получение информации о текущем пользователе");
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Пользователь не аутентифицирован при запросе информации");
            throw new RuntimeException("Пользователь не аутентифицирован");
        }

        User user = (User) authentication.getPrincipal();
        log.debug("Информация о пользователе получена: {}", user.getUsername());
        
        return UserMapper.userToUserLoggedDto(user);
    }
}