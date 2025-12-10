package com.example.demo.jwt;

import java.time.LocalDateTime;
import java.time.temporal.TemporalUnit;
import java.util.Map;

import org.springframework.security.core.userdetails.UserDetails;

import com.example.demo.model.Token;

public interface JwtTokenProvider {
    Token generateAccessToken(Map<String, Object> extraClaims, long duration, TemporalUnit durationType, UserDetails user);
    Token generateRefreshToken(long duration, TemporalUnit durationType, UserDetails user);
    boolean validateToken(String tokenValue);
    String getUsernameFromToken(String tokenValue);
    LocalDateTime getExpiryDateFromToken(String tokenValue);
}