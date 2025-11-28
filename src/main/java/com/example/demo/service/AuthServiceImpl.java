package com.example.demo.service;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.UserLoggedDto;
import com.example.demo.jwt.JwtTokenProvider;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    
    @Override
    public ResponseEntity<LoginResponse> login(LoginRequest loginRequest, String accessToken, String refreshToken) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.username(),
                    loginRequest.password()
                )
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            User user = (User) authentication.getPrincipal();
            String role = user.getRole().getAuthority();
            
            var accessTokenObj = jwtTokenProvider.generateAccessToken(
                null, 15, ChronoUnit.MINUTES, user
            );
            var refreshTokenObj = jwtTokenProvider.generateRefreshToken(
                7, ChronoUnit.DAYS, user
            );
            
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
            
            return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new LoginResponse(true, role));
                
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new LoginResponse(false, ""));
        }
    }
    
    @Override
    public ResponseEntity<LoginResponse> refresh(String refreshToken) {
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.badRequest().body(new LoginResponse(false, ""));
        }
        
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        var newAccessToken = jwtTokenProvider.generateAccessToken(
            null, 15, ChronoUnit.MINUTES, user
        );
        
        ResponseCookie accessCookie = ResponseCookie.from("access_token", newAccessToken.getTokenValue())
            .httpOnly(true)
            .secure(false)
            .path("/")
            .maxAge(15 * 60)
            .build();
            
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
            .body(new LoginResponse(true, user.getRole().getAuthority()));
    }
    
    @Override
    public ResponseEntity<LoginResponse> logout(String accessToken, String refreshToken) {
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
        
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
            .body(new LoginResponse(false, ""));
    }
    
    @Override
    public UserLoggedDto getUserLoggedInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        
        User user = (User) authentication.getPrincipal();
        return UserMapper.userToUserLoggedDto(user);
    }
}