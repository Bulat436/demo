package com.example.demo.service;

import com.example.demo.config.TelegramConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramNotificationService {
    
    private final TelegramConfig telegramConfig;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot{token}/sendMessage";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    

    public void sendAuthNotification(String username, String role, String ipAddress, boolean success) {
        if (!telegramConfig.isEnabled()) {
            log.debug("Уведомления в Telegram отключены");
            return;
        }
        
        try {
            String message = buildAuthMessage(username, role, ipAddress, success);
            sendMessage(message);
            log.debug("Уведомление об аутентификации отправлено в Telegram для пользователя: {}", username);
        } catch (Exception e) {
            log.error("Ошибка отправки уведомления в Telegram", e);
        }
    }
    

    public void sendAuthErrorNotification(String username, String ipAddress, String errorMessage) {
        if (!telegramConfig.isEnabled()) {
            return;
        }
        
        try {
            String message = buildAuthErrorMessage(username, ipAddress, errorMessage);
            sendMessage(message);
            log.debug("Уведомление об ошибке аутентификации отправлено в Telegram для пользователя: {}", username);
        } catch (Exception e) {
            log.error("Ошибка отправки уведомления об ошибке в Telegram", e);
        }
    }
    

    public void sendNotification(String title, String content) {
        if (!telegramConfig.isEnabled()) {
            return;
        }
        
        try {
            String message = buildGeneralMessage(title, content);
            sendMessage(message);
            log.debug("Общее уведомление отправлено в Telegram: {}", title);
        } catch (Exception e) {
            log.error("Ошибка отправки общего уведомления в Telegram", e);
        }
    }
    

    private String buildAuthMessage(String username, String role, String ipAddress, boolean success) {
        String status = success ? "✅ УСПЕШНАЯ" : "❌ НЕУДАЧНАЯ";
        String emoji = success ? "🔓" : "🔒";
        
        return String.format(
            "%s *АУТЕНТИФИКАЦИЯ* %s\n\n" +
            " *Пользователь:* `%s`\n" +
            " *Роль:* `%s`\n" +
            " *IP адрес:* `%s`\n" +
            " *Статус:* %s\n" +
            " *Время:* %s",
            emoji, emoji,
            username,
            role,
            ipAddress,
            status,
            LocalDateTime.now().format(DATE_FORMATTER)
        );
    }
    
    private String buildAuthErrorMessage(String username, String ipAddress, String errorMessage) {
        return String.format(
            " *ОШИБКА АУТЕНТИФИКАЦИИ*\n\n" +
            " *Пользователь:* `%s`\n" +
            " *IP адрес:* `%s`\n" +
            " *Ошибка:* %s\n" +
            " *Время:* %s",
            username,
            ipAddress,
            errorMessage,
            LocalDateTime.now().format(DATE_FORMATTER)
        );
    }
    
    private String buildGeneralMessage(String title, String content) {
        return String.format(
            " *%s*\n\n%s\n\n🕐 *Время:* %s",
            title,
            content,
            LocalDateTime.now().format(DATE_FORMATTER)
        );
    }
    
    private void sendMessage(String text) {
        try {
            String token = telegramConfig.getToken();
            String chatId = telegramConfig.getChatId();
            
            if (token == null || token.isEmpty() || chatId == null || chatId.isEmpty()) {
                log.warn("Не настроены токен или chat_id для Telegram бота");
                return;
            }
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("chat_id", chatId);
            requestBody.put("text", text);
            requestBody.put("parse_mode", "Markdown");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                TELEGRAM_API_URL,
                request,
                String.class,
                token
            );
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Ошибка отправки сообщения в Telegram: {}", response.getBody());
            } else {
                log.debug("Сообщение успешно отправлено в Telegram");
            }
            
        } catch (Exception e) {
            log.error("Исключение при отправке сообщения в Telegram", e);
            throw new RuntimeException("Ошибка отправки сообщения в Telegram: " + e.getMessage(), e);
        }
    }
}