package com.example.demo.service;

import com.example.demo.dto.ReportRequest;
import com.example.demo.model.Alert;
import com.example.demo.enums.StatusType;
import com.example.demo.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfReportService {
    
    private final AlertRepository alertRepository;
    
    public byte[] generateDailyReport(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Генерация ежедневного отчёта за период: {} - {}", startDate, endDate);
        
        try {
            List<Alert> alerts = alertRepository.findAll();
            List<Alert> filteredAlerts = alerts.stream()
                    .filter(alert -> !alert.getTimestamp().isBefore(startDate) 
                            && !alert.getTimestamp().isAfter(endDate))
                    .collect(Collectors.toList());

            long totalAlerts = filteredAlerts.size();
            long newCount = countByStatus(filteredAlerts, StatusType.NEW);
            long inProgressCount = countByStatus(filteredAlerts, StatusType.IN_PROGRESS);
            long resolvedCount = countByStatus(filteredAlerts, StatusType.RESOLVED);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("REPORT_TITLE", "ЕЖЕДНЕВНЫЙ ОТЧЁТ");
            parameters.put("GENERATION_DATE", "Сгенерирован: " + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
            parameters.put("REPORT_DATE", "Дата: " + startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            parameters.put("STATISTICS_TEXT", String.format("Всего инцидентов: %d | Новые: %d | В работе: %d | Решено: %d",
                    totalAlerts, newCount, inProgressCount, resolvedCount));
            parameters.put("TOTAL_ALERTS", totalAlerts);
            
            List<Map<String, Object>> reportData = filteredAlerts.stream()
                    .map(this::createReportRow)
                    .collect(Collectors.toList());
            
            return generateReport("reports/templates/daily_report.jrxml", parameters, reportData);
            
        } catch (Exception e) {
            log.error("Ошибка генерации ежедневного отчёта", e);
            throw new RuntimeException("Ошибка генерации ежедневного отчёта: " + e.getMessage());
        }
    }
    

    public byte[] generateWeeklyReport(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Генерация еженедельного отчёта за период: {} - {}", startDate, endDate);
        
        try {
            List<Alert> alerts = alertRepository.findAll();
            List<Alert> filteredAlerts = alerts.stream()
                    .filter(alert -> !alert.getTimestamp().isBefore(startDate) 
                            && !alert.getTimestamp().isAfter(endDate))
                    .collect(Collectors.toList());
   
            long totalAlerts = filteredAlerts.size();
            long newCount = countByStatus(filteredAlerts, StatusType.NEW);
            long inProgressCount = countByStatus(filteredAlerts, StatusType.IN_PROGRESS);
            long resolvedCount = countByStatus(filteredAlerts, StatusType.RESOLVED);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("REPORT_TITLE", "ЕЖЕНЕДЕЛЬНЫЙ ОТЧЁТ");
            parameters.put("GENERATION_DATE", "Сгенерирован: " + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
            parameters.put("WEEK_PERIOD", "Период: " + startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) 
                    + " - " + endDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            parameters.put("STATISTICS_TEXT", String.format("Всего инцидентов: %d | Новые: %d | В работе: %d | Решено: %d",
                    totalAlerts, newCount, inProgressCount, resolvedCount));
            parameters.put("TOTAL_ALERTS", totalAlerts);

            List<Map<String, Object>> reportData = filteredAlerts.stream()
                    .map(this::createReportRow)
                    .collect(Collectors.toList());

            return generateReport("reports/templates/weekly_report.jrxml", parameters, reportData);
            
        } catch (Exception e) {
            log.error("Ошибка генерации еженедельного отчёта", e);
            throw new RuntimeException("Ошибка генерации еженедельного отчёта: " + e.getMessage());
        }
    }
    
    public byte[] generateMonthlyReport(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Генерация ежемесячного отчёта за период: {} - {}", startDate, endDate);
        
        try {
            List<Alert> alerts = alertRepository.findAll();
            List<Alert> filteredAlerts = alerts.stream()
                    .filter(alert -> !alert.getTimestamp().isBefore(startDate) 
                            && !alert.getTimestamp().isAfter(endDate))
                    .collect(Collectors.toList());
            
            long totalAlerts = filteredAlerts.size();
            long newCount = countByStatus(filteredAlerts, StatusType.NEW);
            long inProgressCount = countByStatus(filteredAlerts, StatusType.IN_PROGRESS);
            long resolvedCount = countByStatus(filteredAlerts, StatusType.RESOLVED);
            
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("REPORT_TITLE", "ЕЖЕМЕСЯЧНЫЙ ОТЧЁТ");
            parameters.put("GENERATION_DATE", "Сгенерирован: " + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
            parameters.put("MONTH_PERIOD", "Месяц: " + startDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", 
                    new Locale("ru", "RU"))) + " (" + startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) 
                    + " - " + endDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + ")");
            parameters.put("STATISTICS_TEXT", String.format("Всего инцидентов: %d | Новые: %d | В работе: %d | Решено: %d",
                    totalAlerts, newCount, inProgressCount, resolvedCount));
            parameters.put("TOTAL_ALERTS", totalAlerts);
            
            List<Map<String, Object>> reportData = filteredAlerts.stream()
                    .map(this::createReportRow)
                    .collect(Collectors.toList());
            

            return generateReport("reports/templates/monthly_report.jrxml", parameters, reportData);
            
        } catch (Exception e) {
            log.error("Ошибка генерации ежемесячного отчёта", e);
            throw new RuntimeException("Ошибка генерации ежемесячного отчёта: " + e.getMessage());
        }
    }
    
    private byte[] generateReport(String templatePath, Map<String, Object> parameters, 
                                 List<Map<String, Object>> reportData) throws JRException {
        InputStream templateStream = loadTemplate(templatePath);
        JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }
    
    private Map<String, Object> createReportRow(Alert alert) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", alert.getId());
        row.put("busId", alert.getBusId());
        row.put("type", alert.getType() != null ? alert.getType().name() : "");
        row.put("timestamp", alert.getTimestamp());
        row.put("location", alert.getLocation() != null ? alert.getLocation() : "");
        row.put("status", alert.getStatus() != null ? alert.getStatus().name() : "");
        row.put("description", alert.getDescription() != null ? alert.getDescription() : "");
        return row;
    }
    
    private InputStream loadTemplate(String templatePath) {
        try {
            return new ClassPathResource(templatePath).getInputStream();
        } catch (Exception e) {
            log.error("Не удалось загрузить шаблон: {}", templatePath, e);
            throw new RuntimeException("Не найден шаблон отчёта: " + templatePath);
        }
    }
    
    private long countByStatus(List<Alert> alerts, StatusType status) {
        return alerts.stream()
                .filter(alert -> alert.getStatus() == status)
                .count();
    }
    
    public Map<String, Object> getReportStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        List<Alert> allAlerts = alertRepository.findAll();
        List<Alert> filteredAlerts = allAlerts.stream()
                .filter(alert -> !alert.getTimestamp().isBefore(startDate) 
                        && !alert.getTimestamp().isAfter(endDate))
                .collect(Collectors.toList());
        
        long newCount = countByStatus(filteredAlerts, StatusType.NEW);
        long inProgressCount = countByStatus(filteredAlerts, StatusType.IN_PROGRESS);
        long resolvedCount = countByStatus(filteredAlerts, StatusType.RESOLVED);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAlerts", filteredAlerts.size());
        stats.put("newCount", newCount);
        stats.put("inProgressCount", inProgressCount);
        stats.put("resolvedCount", resolvedCount);
        stats.put("periodStart", startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        stats.put("periodEnd", endDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        stats.put("statisticsText", String.format("Всего: %d | Новые: %d | В работе: %d | Решено: %d",
                filteredAlerts.size(), newCount, inProgressCount, resolvedCount));
        
        return stats;
    }
    
    public byte[] generateTestReport() {
        try {
            List<Alert> alerts = alertRepository.findAll();
            
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("REPORT_TITLE", "ТЕСТОВЫЙ ОТЧЁТ");
            parameters.put("GENERATION_DATE", "Сгенерирован: " + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
            parameters.put("TOTAL_ALERTS", alerts.size());
            
            List<Map<String, Object>> reportData = alerts.stream()
                    .map(this::createReportRow)
                    .collect(Collectors.toList());
            
            return generateReport("reports/templates/minimal_report.jrxml", parameters, reportData);
            
        } catch (Exception e) {
            log.error("Ошибка генерации тестового отчёта", e);
            throw new RuntimeException("Ошибка генерации тестового отчёта: " + e.getMessage());
        }
    }
    
    public byte[] generateCustomReport(ReportRequest request) {
        return generateFilteredPdfReport(request);
    }
    
    public byte[] generateSimplePdfReport(ReportRequest request) {
        return generateTestReport();
    }
    
    public byte[] generateFilteredPdfReport(ReportRequest request) {
        try {
            LocalDateTime startDate = request.getStartDate() != null ? 
                    request.getStartDate() : LocalDateTime.now().minusDays(30);
            LocalDateTime endDate = request.getEndDate() != null ? 
                    request.getEndDate() : LocalDateTime.now();
            
            List<Alert> alerts = alertRepository.findAll();
            List<Alert> filteredAlerts = alerts.stream()
                    .filter(alert -> !alert.getTimestamp().isBefore(startDate) 
                            && !alert.getTimestamp().isAfter(endDate))
                    .collect(Collectors.toList());
            
            // Статистика
            long totalAlerts = filteredAlerts.size();
            long newCount = countByStatus(filteredAlerts, StatusType.NEW);
            long inProgressCount = countByStatus(filteredAlerts, StatusType.IN_PROGRESS);
            long resolvedCount = countByStatus(filteredAlerts, StatusType.RESOLVED);
            
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("REPORT_TITLE", getReportTitle(request.getReportType()));
            parameters.put("GENERATION_DATE", "Сгенерирован: " + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
            parameters.put("PERIOD_START", "Период: " + formatDate(startDate));
            parameters.put("PERIOD_END", formatDate(endDate));
            parameters.put("TOTAL_ALERTS", totalAlerts);
            parameters.put("NEW_COUNT", newCount);
            parameters.put("IN_PROGRESS_COUNT", inProgressCount);
            parameters.put("RESOLVED_COUNT", resolvedCount);
            parameters.put("STATISTICS_TEXT", String.format("Всего: %d | Новые: %d | В работе: %d | Решено: %d",
                    totalAlerts, newCount, inProgressCount, resolvedCount));
            
            List<Map<String, Object>> reportData = filteredAlerts.stream()
                    .map(this::createReportRow)
                    .collect(Collectors.toList());
            
            return generateReport("reports/templates/default_report.jrxml", parameters, reportData);
            
        } catch (Exception e) {
            log.error("Ошибка генерации фильтрованного PDF", e);
            throw new RuntimeException("Ошибка: " + e.getMessage());
        }
    }
    
    private String getReportTitle(ReportRequest.ReportType reportType) {
        if (reportType == null) {
            return "Отчёт по инцидентам";
        }
        switch (reportType) {
            case DAILY: return "Ежедневный отчёт";
            case WEEKLY: return "Еженедельный отчёт";
            case MONTHLY: return "Ежемесячный отчёт";
            case CUSTOM: return "Пользовательский отчёт";
            default: return "Отчёт по инцидентам";
        }
    }
    
    private String formatDate(LocalDateTime date) {
        if (date == null) return "Не указано";
        return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }
}