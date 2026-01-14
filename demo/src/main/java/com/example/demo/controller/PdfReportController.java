package com.example.demo.controller;

import com.example.demo.dto.ReportRequest;
import com.example.demo.service.PdfReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Tag(name = "PDF Reports", description = "API для генерации PDF отчётов")
@RestController
@RequestMapping("/api/reports/pdf")
@RequiredArgsConstructor
public class PdfReportController {
    
    // Константы
    private static final String CONTENT_DISPOSITION_PREFIX = "attachment; filename=\"";
    private static final String CONTENT_DISPOSITION_SUFFIX = "\"";
    private static final String ERROR_KEY = "error";
    private static final String MESSAGE_KEY = "message";
    private static final String REPORT_GENERATION_ERROR = "Ошибка генерации отчёта";
    private static final String DAILY_REPORT_ERROR = "Ошибка генерации ежедневного отчёта";
    private static final String WEEKLY_REPORT_ERROR = "Ошибка генерации еженедельного отчёта";
    private static final String MONTHLY_REPORT_ERROR = "Ошибка генерации ежемесячного отчёта";
    private static final String CUSTOM_REPORT_ERROR = "Ошибка генерации пользовательского отчёта";
    private static final String DATE_FORMAT_ERROR = "Некорректный формат даты";
    private static final String DATE_FORMAT_MESSAGE = "Используйте формат: YYYY-MM-DD";
    private static final String STATISTICS_ERROR = "Ошибка получения статистики";
    
    private final PdfReportService pdfReportService;
    
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    @Operation(summary = "Тестовый отчёт", description = "Простой отчёт для тестирования генерации PDF")
    @GetMapping("/test")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<byte[]> generateTestReport() {
        log.info("Запрос тестового отчёта");
        
        try {
            byte[] pdfBytes = pdfReportService.generateTestReport();
            String fileName = "test_report_" + LocalDateTime.now().format(FILE_DATE_FORMATTER) + ".pdf";
            return buildPdfResponse(fileName, pdfBytes);
            
        } catch (Exception e) {
            log.error("Ошибка генерации тестового отчёта", e);
            return buildErrorResponse(REPORT_GENERATION_ERROR, e.getMessage());
        }
    }
    
    @Operation(summary = "Ежедневный отчёт", description = "Отчёт за вчерашний день")
    @GetMapping("/daily")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<byte[]> generateDailyReport() {
        log.info("Запрос ежедневного отчёта");
        
        try {
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            LocalDateTime startDate = yesterday.withHour(0).withMinute(0).withSecond(0);
            LocalDateTime endDate = yesterday.withHour(23).withMinute(59).withSecond(59);
            
            byte[] pdfBytes = pdfReportService.generateDailyReport(startDate, endDate);
            String fileName = "daily_report_" + yesterday.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";
            
            return buildPdfResponse(fileName, pdfBytes);
            
        } catch (Exception e) {
            log.error("Ошибка генерации ежедневного отчёта", e);
            return buildErrorResponse(DAILY_REPORT_ERROR, e.getMessage());
        }
    }
    
    @Operation(summary = "Ежедневный отчёт за конкретную дату", 
               description = "Отчёт за указанную дату в формате YYYY-MM-DD")
    @GetMapping("/daily/{date}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<byte[]> generateDailyReportForDate(@PathVariable String date) {
        log.info("Запрос ежедневного отчёта за дату: {}", date);
        
        try {
            LocalDate reportDate = LocalDate.parse(date, DATE_FORMATTER);
            LocalDateTime startDate = reportDate.atStartOfDay();
            LocalDateTime endDate = reportDate.atTime(23, 59, 59);
            
            byte[] pdfBytes = pdfReportService.generateDailyReport(startDate, endDate);
            String fileName = "daily_report_" + reportDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";
            
            return buildPdfResponse(fileName, pdfBytes);
            
        } catch (DateTimeParseException e) {
            log.error("Ошибка парсинга даты", e);
            return buildErrorResponse(DATE_FORMAT_ERROR, DATE_FORMAT_MESSAGE);
        } catch (Exception e) {
            log.error("Ошибка генерации ежедневного отчёта", e);
            return buildErrorResponse(DAILY_REPORT_ERROR, e.getMessage());
        }
    }
    
    @Operation(summary = "Еженедельный отчёт", description = "Отчёт за предыдущую неделю")
    @GetMapping("/weekly")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<byte[]> generateWeeklyReport() {
        log.info("Запрос еженедельного отчёта");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endDate = now.minusDays(1).withHour(23).withMinute(59).withSecond(59);
            LocalDateTime startDate = endDate.minusDays(6).withHour(0).withMinute(0).withSecond(0);
            
            byte[] pdfBytes = pdfReportService.generateWeeklyReport(startDate, endDate);
            String fileName = "weekly_report_" + startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) 
                    + "_" + endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";
            
            return buildPdfResponse(fileName, pdfBytes);
            
        } catch (Exception e) {
            log.error("Ошибка генерации еженедельного отчёта", e);
            return buildErrorResponse(WEEKLY_REPORT_ERROR, e.getMessage());
        }
    }
    
    @Operation(summary = "Еженедельный отчёт за конкретную неделю", 
               description = "Отчёт за неделю, содержащую указанную дату в формате YYYY-MM-DD")
    @GetMapping("/weekly/{date}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<byte[]> generateWeeklyReportForDate(@PathVariable String date) {
        log.info("Запрос еженедельного отчёта для недели, содержащей дату: {}", date);
        
        try {
            LocalDate weekDate = LocalDate.parse(date, DATE_FORMATTER);
            LocalDateTime startDate = weekDate.minusDays(weekDate.getDayOfWeek().getValue() - 1)
                    .atStartOfDay();
            LocalDateTime endDate = startDate.plusDays(6).withHour(23).withMinute(59).withSecond(59);
            
            byte[] pdfBytes = pdfReportService.generateWeeklyReport(startDate, endDate);
            String fileName = "weekly_report_" + startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) 
                    + "_" + endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";
            
            return buildPdfResponse(fileName, pdfBytes);
            
        } catch (DateTimeParseException e) {
            log.error("Ошибка парсинга даты", e);
            return buildErrorResponse(DATE_FORMAT_ERROR, DATE_FORMAT_MESSAGE);
        } catch (Exception e) {
            log.error("Ошибка генерации еженедельного отчёта", e);
            return buildErrorResponse(WEEKLY_REPORT_ERROR, e.getMessage());
        }
    }
    
    @Operation(summary = "Ежемесячный отчёт", description = "Отчёт за предыдущий месяц")
    @GetMapping("/monthly")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<byte[]> generateMonthlyReport() {
        log.info("Запрос ежемесячного отчёта");
        
        try {
            // Предыдущий месяц
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime firstDayOfLastMonth = now.minusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime lastDayOfLastMonth = firstDayOfLastMonth.plusMonths(1).minusDays(1)
                    .withHour(23).withMinute(59).withSecond(59);
            
            byte[] pdfBytes = pdfReportService.generateMonthlyReport(firstDayOfLastMonth, lastDayOfLastMonth);
            String fileName = "monthly_report_" + firstDayOfLastMonth.format(DateTimeFormatter.ofPattern("yyyyMM")) + ".pdf";
            
            return buildPdfResponse(fileName, pdfBytes);
            
        } catch (Exception e) {
            log.error("Ошибка генерации ежемесячного отчёта", e);
            return buildErrorResponse(MONTHLY_REPORT_ERROR, e.getMessage());
        }
    }
    
    @Operation(summary = "Ежемесячный отчёт за конкретный месяц", 
               description = "Отчёт за указанный месяц в формате YYYY-MM")
    @GetMapping("/monthly/{yearMonth}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<byte[]> generateMonthlyReportForMonth(@PathVariable String yearMonth) {
        log.info("Запрос ежемесячного отчёта за месяц: {}", yearMonth);
        
        try {
            LocalDate monthDate = LocalDate.parse(yearMonth + "-01", DATE_FORMATTER);
            LocalDateTime firstDayOfMonth = monthDate.atStartOfDay();
            LocalDateTime lastDayOfMonth = monthDate.plusMonths(1).minusDays(1)
                    .atTime(23, 59, 59);
            
            byte[] pdfBytes = pdfReportService.generateMonthlyReport(firstDayOfMonth, lastDayOfMonth);
            String fileName = "monthly_report_" + yearMonth.replace("-", "") + ".pdf";
            
            return buildPdfResponse(fileName, pdfBytes);
            
        } catch (DateTimeParseException e) {
            log.error("Ошибка парсинга даты", e);
            return buildErrorResponse(DATE_FORMAT_ERROR, "Используйте формат: YYYY-MM (например: 2024-12)");
        } catch (Exception e) {
            log.error("Ошибка генерации ежемесячного отчёта", e);
            return buildErrorResponse(MONTHLY_REPORT_ERROR, e.getMessage());
        }
    }
    
    @Operation(summary = "Отчёт за произвольный период", 
               description = "Отчёт за указанные даты в формате YYYY-MM-DD")
    @GetMapping("/custom")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<byte[]> generateCustomReport(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        
        log.info("Запрос отчёта за период: {} - {}", startDate, endDate);
        
        try {
            LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
            LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);
            
            LocalDateTime startDateTime = start.atStartOfDay();
            LocalDateTime endDateTime = end.atTime(23, 59, 59);
            
            if (startDateTime.isAfter(endDateTime)) {
                return buildErrorResponse("Некорректный период", "Дата начала должна быть раньше даты окончания");
            }
            
            ReportRequest request = new ReportRequest();
            request.setReportType(ReportRequest.ReportType.CUSTOM);
            request.setStartDate(startDateTime);
            request.setEndDate(endDateTime);
            
            byte[] pdfBytes = pdfReportService.generateCustomReport(request);
            String fileName = String.format("custom_report_%s_%s_%s.pdf", 
                startDate, endDate, LocalDateTime.now().format(FILE_DATE_FORMATTER));
            
            return buildPdfResponse(fileName, pdfBytes);
            
        } catch (DateTimeParseException e) {
            log.error("Ошибка парсинга даты", e);
            return buildErrorResponse(DATE_FORMAT_ERROR, DATE_FORMAT_MESSAGE);
        } catch (Exception e) {
            log.error("Ошибка генерации пользовательского отчёта", e);
            return buildErrorResponse(CUSTOM_REPORT_ERROR, e.getMessage());
        }
    }
    
    @Operation(summary = "Статистика за период", 
               description = "Получение статистики инцидентов без генерации PDF")
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getStatistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        log.info("Запрос статистики: {} - {}", startDate, endDate);
        
        try {
            LocalDateTime startDateTime;
            LocalDateTime endDateTime;
            
            if (startDate != null && endDate != null) {
                LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
                LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);
                startDateTime = start.atStartOfDay();
                endDateTime = end.atTime(23, 59, 59);
            } else {
                endDateTime = LocalDateTime.now();
                startDateTime = endDateTime.minusDays(30);
            }
            
            Map<String, Object> statistics = pdfReportService.getReportStatistics(startDateTime, endDateTime);
            return ResponseEntity.ok(statistics);
            
        } catch (DateTimeParseException e) {
            log.error("Ошибка парсинга даты", e);
            return buildStatisticsErrorResponse(DATE_FORMAT_ERROR, DATE_FORMAT_MESSAGE);
        } catch (Exception e) {
            log.error("Ошибка получения статистики", e);
            return buildStatisticsErrorResponse(STATISTICS_ERROR, e.getMessage());
        }
    }
    
    
    private ResponseEntity<byte[]> buildPdfResponse(String fileName, byte[] pdfBytes) {
        String contentDisposition = CONTENT_DISPOSITION_PREFIX + fileName + CONTENT_DISPOSITION_SUFFIX;
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
    
    private ResponseEntity<byte[]> buildErrorResponse(String error, String message) {
        Map<String, String> errorMap = new HashMap<>();
        errorMap.put(ERROR_KEY, error);
        errorMap.put(MESSAGE_KEY, message);
        
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorMap.toString().getBytes());
    }
    
    private ResponseEntity<Map<String, Object>> buildStatisticsErrorResponse(String error, String message) {
        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put(ERROR_KEY, error);
        errorMap.put(MESSAGE_KEY, message);
        
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorMap);
    }
}