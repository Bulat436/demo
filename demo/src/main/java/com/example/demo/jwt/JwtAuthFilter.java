package com.example.demo.jwt;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.demo.service.UserDetailsServiceImpl;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Value("${jwt.access-cookie-name:access_token}")
    private String accessTokenCookieName;

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
    @NonNull HttpServletResponse response, 
    @NonNull FilterChain filterChain) throws ServletException, IOException {
        log.debug("JWT фильтр обрабатывает запрос: {} {}", 
                 request.getMethod(), request.getRequestURI());
        
        String accessToken = getJwtFromCookie(request);

        if(accessToken == null) {
            log.debug("Access токен не найден в cookies");
            filterChain.doFilter(request, response);
            return;
        }

        if(!tokenProvider.validateToken(accessToken)) {
            log.warn("Предоставлен неверный access токен");
            filterChain.doFilter(request, response);
            return;
        }

        String username = tokenProvider.getUsernameFromToken(accessToken);

        if(username == null) {
            log.warn("Не удалось извлечь имя пользователя из токена");
            filterChain.doFilter(request, response);
            return;
        }

        log.debug("Валидный JWT токен для пользователя: {}", username);
        
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );

            authenticationToken.setDetails(new WebAuthenticationDetailsSource()
            .buildDetails(request));
            SecurityContextHolder.getContext()
            .setAuthentication(authenticationToken);
            
            log.debug("Аутентификация установлена для пользователя: {}", username);
            
        } catch (UsernameNotFoundException e) {
            log.warn("Пользователь не найден для валидного токена: {}", username);
        } catch (Exception e) {
            log.error("Ошибка установки аутентификации: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if(cookies == null) {
            return null;
        }
        
        for (Cookie cookie : cookies) {
            if (accessTokenCookieName.equals(cookie.getName())) {
                log.trace("Найден access токен cookie");
                return cookie.getValue();
            }
        }
        
        return null;
    }
}