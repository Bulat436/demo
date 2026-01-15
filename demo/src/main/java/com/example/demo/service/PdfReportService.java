package com.example.demo.service;

import com.example.demo.dto.ReportRequest;
import com.example.demo.model.Alert;
import com.example.demo.model.EventType;
import com.example.demo.model.StatusType;
import com.example.demo.repository.AlertRepository;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfReportService {
    
    private final AlertRepository alertRepository;
    
    private static final float HEADER_FONT_SIZE = 20f;
    private static final float SUBHEADER_FONT_SIZE = 14f;
    private static final float BODY_FONT_SIZE = 10f;
    private static final float SMALL_FONT_SIZE = 8f;
    private static final float MARGIN = 50f;
    
    private static final DateTimeFormatter DATE_TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss", new Locale("ru"));
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("dd.MM.yyyy", new Locale("ru"));
    
    public byte[] generateDailyReport(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Генерация ежедневного отчета за период: {} - {}", startDate, endDate);
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            List<Alert> alerts = alertRepository.findByTimestampBetween(startDate, endDate);
            
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = initDocument(pdfDoc);
            
            PdfFont russianFont = getRussianFont();
            
            addHeader(document, "ЕЖЕДНЕВНЫЙ ОТЧЕТ ПО ИНЦИДЕНТАМ", russianFont);
            addReportPeriod(document, startDate, endDate, russianFont);
            
            addStatisticsSection(document, alerts, startDate, endDate, russianFont);
            
            if (!alerts.isEmpty()) {
                addAlertsTable(document, alerts, "Детализация инцидентов за день", russianFont);
            } else {
                addNoDataMessage(document, russianFont);
            }
            
            addEventTypeStatistics(document, alerts, russianFont);
            
            addFooter(document, russianFont);
            
            document.close();
            pdfDoc.close();
            log.info("Ежедневный отчет успешно сгенерирован, количество инцидентов: {}", alerts.size());
            return baos.toByteArray();
            
        } catch (Exception e) {
            log.error("Ошибка генерации ежедневного отчета", e);
            throw new ReportGenerationException("Ошибка генерации отчета", e);
        }
    }
    
    public byte[] generateWeeklyReport(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Генерация еженедельного отчета за период: {} - {}", startDate, endDate);
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            List<Alert> alerts = alertRepository.findByTimestampBetween(startDate, endDate);
            
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = initDocument(pdfDoc);

            PdfFont russianFont = getRussianFont();

            addHeader(document, "ЕЖЕНЕДЕЛЬНЫЙ ОТЧЕТ ПО ИНЦИДЕНТАМ", russianFont);
            addReportPeriod(document, startDate, endDate, russianFont);

            addStatisticsSection(document, alerts, startDate, endDate, russianFont);

            addTopBusesTable(document, alerts, russianFont);

            if (!alerts.isEmpty()) {
                List<Alert> recentAlerts = alerts.stream()
                    .sorted(Comparator.comparing(Alert::getTimestamp).reversed())
                    .limit(50)
                    .collect(Collectors.toList());
                addAlertsTable(document, recentAlerts, "Последние 50 инцидентов (полный список: " + alerts.size() + ")", russianFont);
            } else {
                addNoDataMessage(document, russianFont);
            }
            
            addFooter(document, russianFont);
            document.close();
            pdfDoc.close();
            
            log.info("Еженедельный отчет успешно сгенерирован, количество инцидентов: {}", alerts.size());
            return baos.toByteArray();
            
        } catch (Exception e) {
            log.error("Ошибка генерации еженедельного отчета", e);
            throw new ReportGenerationException("Ошибка генерации отчета", e);
        }
    }
    
    public byte[] generateMonthlyReport(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Генерация ежемесячного отчета за период: {} - {}", startDate, endDate);
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            List<Alert> alerts = alertRepository.findByTimestampBetween(startDate, endDate);
            
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = initDocument(pdfDoc);
            
            PdfFont russianFont = getRussianFont();
            
            addHeader(document, "ЕЖЕМЕСЯЧНЫЙ ОТЧЕТ ПО ИНЦИДЕНТАМ", russianFont);
            addReportPeriod(document, startDate, endDate, russianFont);
            
            addStatisticsSection(document, alerts, startDate, endDate, russianFont);
            
            addEventTypeStatistics(document, alerts, russianFont);
            
            addTopBusesTable(document, alerts, russianFont);
            
            if (!alerts.isEmpty()) {
                List<Alert> criticalAlerts = alerts.stream()
                    .filter(a -> a.getStatus() != StatusType.RESOLVED)
                    .sorted(Comparator.comparing(Alert::getTimestamp).reversed())
                    .limit(30)
                    .collect(Collectors.toList());
                
                if (!criticalAlerts.isEmpty()) {
                    addAlertsTable(document, criticalAlerts, "Требуют внимания (не решены)", russianFont);
                }
            }

            addRecommendationsSection(document, alerts, russianFont);
            
            addFooter(document, russianFont);
            document.close();
            pdfDoc.close();
            
            log.info("Ежемесячный отчет успешно сгенерирован, количество инцидентов: {}", alerts.size());
            return baos.toByteArray();
            
        } catch (Exception e) {
            log.error("Ошибка генерации ежемесячного отчета", e);
            throw new ReportGenerationException("Ошибка генерации отчета", e);
        }
    }
    
    public byte[] generateCustomReport(ReportRequest request) {
        log.info("Генерация пользовательского отчета типа: {}", request.getReportType());
        
        LocalDateTime startDate = request.getStartDate();
        LocalDateTime endDate = request.getEndDate();
        
        if (startDate == null) startDate = LocalDateTime.now().minusDays(30);
        if (endDate == null) endDate = LocalDateTime.now();
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            List<Alert> alerts = alertRepository.findByTimestampBetween(startDate, endDate);

            if (request.getBusIds() != null && !request.getBusIds().isEmpty()) {
                alerts = alerts.stream()
                    .filter(a -> request.getBusIds().contains(a.getBusId()))
                    .collect(Collectors.toList());
            }
            
            if (request.getStatuses() != null && !request.getStatuses().isEmpty()) {
                alerts = alerts.stream()
                    .filter(a -> request.getStatuses().contains(a.getStatus().name()))
                    .collect(Collectors.toList());
            }
            
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = initDocument(pdfDoc);

            PdfFont russianFont = getRussianFont();

            String title = "ПОЛЬЗОВАТЕЛЬСКИЙ ОТЧЕТ ПО ИНЦИДЕНТАМ";
            if (request.getReportType() != null) {
                switch (request.getReportType()) {
                    case DAILY -> title = "ЕЖЕДНЕВНЫЙ ОТЧЕТ";
                    case WEEKLY -> title = "ЕЖЕНЕДЕЛЬНЫЙ ОТЧЕТ";
                    case MONTHLY -> title = "ЕЖЕМЕСЯЧНЫЙ ОТЧЕТ";
                    case CUSTOM -> title = "ПОЛЬЗОВАТЕЛЬСКИЙ ОТЧЕТ";
                }
            }
            addHeader(document, title, russianFont);
            addReportPeriod(document, startDate, endDate, russianFont);

            if (request.getBusIds() != null && !request.getBusIds().isEmpty()) {
                Paragraph filterInfo = createRussianParagraph("Фильтр по автобусам: " + 
                    request.getBusIds().stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(", ")), russianFont)
                    .setFontSize(SMALL_FONT_SIZE)
                    .setFontColor(ColorConstants.GRAY);
                document.add(filterInfo);
            }
            
            if (request.getStatuses() != null && !request.getStatuses().isEmpty()) {
                Paragraph statusInfo = createRussianParagraph("Фильтр по статусам: " + 
                    String.join(", ", request.getStatuses()), russianFont)
                    .setFontSize(SMALL_FONT_SIZE)
                    .setFontColor(ColorConstants.GRAY);
                document.add(statusInfo);
            }
            
            document.add(createRussianParagraph("\n", russianFont));

            addStatisticsSection(document, alerts, startDate, endDate, russianFont);

            if (!alerts.isEmpty()) {
                addAlertsTable(document, alerts, "Детализация инцидентов", russianFont);
            } else {
                addNoDataMessage(document, russianFont);
            }
            
            addFooter(document, russianFont);
            document.close();
            pdfDoc.close();
            
            log.info("Пользовательский отчет успешно сгенерирован, количество инцидентов: {}", alerts.size());
            return baos.toByteArray();
            
        } catch (Exception e) {
            log.error("Ошибка генерации пользовательского отчета", e);
            throw new ReportGenerationException("Ошибка генерации отчета", e);
        }
    }
    
    public byte[] generateTestReport() {
        log.info("Генерация тестового отчета");
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            List<Alert> alerts = alertRepository.findAll();
            
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = initDocument(pdfDoc);

            PdfFont russianFont = getRussianFont();
            
            addHeader(document, "ТЕСТОВЫЙ ОТЧЕТ СИСТЕМЫ", russianFont);
            
            String reportInfo = String.format(
                "Это тестовый отчет системы мониторинга инцидентов.%n" +
                "Отчет демонстрирует возможности генерации PDF документов.%n" +
                "Дата генерации: %s%n",
                LocalDateTime.now().format(DATE_TIME_FORMATTER)
            );
            
            Paragraph info = createRussianParagraph(reportInfo, russianFont)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(BODY_FONT_SIZE);
            document.add(info);
            
            document.add(createRussianParagraph("\n", russianFont));
            
            if (!alerts.isEmpty()) {
                String statsText = String.format(
                    "Всего инцидентов в системе: %d%n" +
                    "Из них:%n" +
                    "  • Новые: %d%n" +
                    "  • В работе: %d%n" +
                    "  • Решены: %d",
                    alerts.size(),
                    countByStatus(alerts, StatusType.NEW),
                    countByStatus(alerts, StatusType.IN_PROGRESS),
                    countByStatus(alerts, StatusType.RESOLVED)
                );
                
                Paragraph stats = createRussianParagraph(statsText, russianFont)
                    .setFontSize(BODY_FONT_SIZE);
                
                document.add(stats);
                
                document.add(createRussianParagraph("\nПример данных (первые 5 записей):", russianFont)
                    .setBold()
                    .setFontSize(SUBHEADER_FONT_SIZE));
                
                Table sampleTable = new Table(UnitValue.createPercentArray(new float[]{1, 2, 3, 2}))
                    .useAllAvailableWidth()
                    .setMarginTop(10)
                    .setMarginBottom(20);

                sampleTable.addHeaderCell(createHeaderCell("ID", russianFont));
                sampleTable.addHeaderCell(createHeaderCell("Автобус", russianFont));
                sampleTable.addHeaderCell(createHeaderCell("Тип", russianFont));
                sampleTable.addHeaderCell(createHeaderCell("Статус", russianFont));

                alerts.stream().limit(5).forEach(alert -> {
                    sampleTable.addCell(createCell(String.valueOf(alert.getId()), russianFont));
                    sampleTable.addCell(createCell(String.valueOf(alert.getBusId()), russianFont));
                    sampleTable.addCell(createCell(translateEventType(alert.getType()), russianFont));
                    sampleTable.addCell(createStatusCell(alert.getStatus(), russianFont));
                });
                
                document.add(sampleTable);
            } else {
                document.add(createRussianParagraph("В системе нет инцидентов.", russianFont)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(BODY_FONT_SIZE)
                    .setFontColor(ColorConstants.GRAY));
            }
            
            addFooter(document, russianFont);
            document.close();
            pdfDoc.close();
            
            log.info("Тестовый отчет успешно сгенерирован");
            return baos.toByteArray();
            
        } catch (Exception e) {
            log.error("Ошибка генерации тестового отчета", e);
            throw new ReportGenerationException("Ошибка генерации тестового отчета", e);
        }
    }
    
    public Map<String, Object> getReportStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        List<Alert> alerts = alertRepository.findByTimestampBetween(startDate, endDate);
        
        Map<EventType, Long> byEventType = alerts.stream()
            .collect(Collectors.groupingBy(Alert::getType, Collectors.counting()));
        
        Map<StatusType, Long> byStatus = alerts.stream()
            .collect(Collectors.groupingBy(Alert::getStatus, Collectors.counting()));
        
        Map<Long, Long> byBus = alerts.stream()
            .collect(Collectors.groupingBy(Alert::getBusId, Collectors.counting()));
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalAlerts", alerts.size());
        result.put("startDate", startDate.format(DATE_FORMATTER));
        result.put("endDate", endDate.format(DATE_FORMATTER));
        result.put("byEventType", byEventType);
        result.put("byStatus", byStatus);
        result.put("byBus", byBus);
        result.put("newCount", countByStatus(alerts, StatusType.NEW));
        result.put("inProgressCount", countByStatus(alerts, StatusType.IN_PROGRESS));
        result.put("resolvedCount", countByStatus(alerts, StatusType.RESOLVED));
        result.put("generationTime", LocalDateTime.now().format(DATE_TIME_FORMATTER));
        
        return result;
    }

    private Document initDocument(PdfDocument pdfDoc) {
        pdfDoc.setDefaultPageSize(PageSize.A4);
        Document document = new Document(pdfDoc);
        document.setMargins(MARGIN, MARGIN, MARGIN, MARGIN);
        return document;
    }

    private PdfFont getRussianFont() {
        try {
            Resource resource = new ClassPathResource("fonts/arial.ttf");
            if (resource.exists()) {
                try (InputStream fontStream = resource.getInputStream()) {
                    byte[] fontBytes = fontStream.readAllBytes();
                    PdfFont font = PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H);
                    log.info("Шрифт успешно загружен из ресурсов");
                    return font;
                }
            }
        } catch (Exception e) {
            log.warn("Не удалось загрузить шрифт из ресурсов: {}", e.getMessage());
        }
        
        String[] fontNames = {
            "Times New Roman",      // Windows
            "Arial",                // Windows
        };
        
        for (String fontName : fontNames) {
            try {
                PdfFont font = PdfFontFactory.createFont(fontName, PdfEncodings.IDENTITY_H);
                log.info("Используется шрифт: {}", fontName);
                return font;
            } catch (Exception e) {
                log.debug("Шрифт {} не найден: {}", fontName, e.getMessage());
            }
        }
        
        try {
            PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            log.warn("Используется стандартный шрифт Helvetica (возможны проблемы с кириллицей)");
            return font;
        } catch (Exception e) {
            throw new ReportGenerationException("Не удалось создать ни один шрифт", e);
        }
    }
    
    private Paragraph createRussianParagraph(String text, PdfFont font) {
        try {
            if (font != null) {
                return new Paragraph(text).setFont(font);
            }
            return new Paragraph(text);
        } catch (Exception e) {
            log.error("Ошибка создания параграфа", e);
            return new Paragraph(text);
        }
    }
    

    private void addHeader(Document document, String title, PdfFont font) {
        try {
            Paragraph header = createRussianParagraph(title, font)
                .setFontSize(HEADER_FONT_SIZE)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
            
            document.add(header);
            
            Div line = new Div();
            line.setBorder(new SolidBorder(ColorConstants.BLACK, 1));
            line.setHeight(1);
            document.add(line);
            
            document.add(createRussianParagraph("\n", font));
            
        } catch (Exception e) {
            log.error("Ошибка при добавлении заголовка", e);
            Paragraph header = new Paragraph(title)
                .setFontSize(HEADER_FONT_SIZE)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
            document.add(header);
        }
    }
    
    private void addReportPeriod(Document document, LocalDateTime startDate, LocalDateTime endDate, PdfFont font) {
        try {
            Paragraph period = createRussianParagraph(
                    String.format("Период: %s - %s",
                        startDate.format(DATE_FORMATTER),
                        endDate.format(DATE_FORMATTER)), font)
                .setFontSize(SMALL_FONT_SIZE)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.DARK_GRAY)
                .setMarginBottom(10);
            
            document.add(period);
        } catch (Exception e) {
            log.error("Ошибка при добавлении периода", e);
            Paragraph period = new Paragraph(
                    String.format("Период: %s - %s",
                        startDate.format(DATE_FORMATTER),
                        endDate.format(DATE_FORMATTER)))
                .setFontSize(SMALL_FONT_SIZE)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.DARK_GRAY)
                .setMarginBottom(10);
            document.add(period);
        }
    }
    
    private void addStatisticsSection(Document document, List<Alert> alerts, 
                                     LocalDateTime startDate, LocalDateTime endDate, PdfFont font) {
        try {
            document.add(createRussianParagraph("СТАТИСТИКА", font)
                .setBold()
                .setFontSize(SUBHEADER_FONT_SIZE)
                .setMarginBottom(10)
                .setMarginTop(20));
            
            long total = alerts.size();
            long newCount = countByStatus(alerts, StatusType.NEW);
            long inProgressCount = countByStatus(alerts, StatusType.IN_PROGRESS);
            long resolvedCount = countByStatus(alerts, StatusType.RESOLVED);
            
            float resolvedPercentage = total > 0 ? (float) resolvedCount / total * 100 : 0;
            
            Table statsTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth()
                .setMarginBottom(20);
            
            statsTable.addCell(createStatCell("Всего инцидентов:", true, font));
            statsTable.addCell(createStatCell(String.valueOf(total), false, font));
            
            statsTable.addCell(createStatCell("Новые:", true, font));
            statsTable.addCell(createStatCell(String.valueOf(newCount), false, font));
            
            statsTable.addCell(createStatCell("В работе:", true, font));
            statsTable.addCell(createStatCell(String.valueOf(inProgressCount), false, font));
            
            statsTable.addCell(createStatCell("Решены:", true, font));
            statsTable.addCell(createStatCell(String.valueOf(resolvedCount) + 
                String.format(" (%.1f%%)", resolvedPercentage), false, font));
            
            if (total > 0) {
                long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1;
                float averagePerDay = (float) total / daysBetween;
                
                statsTable.addCell(createStatCell("Среднее в день:", true, font));
                statsTable.addCell(createStatCell(String.format("%.1f", averagePerDay), false, font));
            }
            
            document.add(statsTable);
            
        } catch (Exception e) {
            log.error("Ошибка при добавлении статистики", e);
            document.add(new Paragraph("СТАТИСТИКА")
                .setBold()
                .setFontSize(SUBHEADER_FONT_SIZE)
                .setMarginBottom(10)
                .setMarginTop(20));
        }
    }
    
    private void addAlertsTable(Document document, List<Alert> alerts, String title, PdfFont font) {
        try {
            document.add(createRussianParagraph(title, font)
                .setBold()
                .setFontSize(SUBHEADER_FONT_SIZE)
                .setMarginBottom(10)
                .setMarginTop(20));
            
            Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1, 2, 2, 2, 2, 2}))
                .useAllAvailableWidth()
                .setMarginBottom(30);
            
            // Заголовки столбцов
            table.addHeaderCell(createHeaderCell("ID", font));
            table.addHeaderCell(createHeaderCell("Автобус", font));
            table.addHeaderCell(createHeaderCell("Тип события", font));
            table.addHeaderCell(createHeaderCell("Время", font));
            table.addHeaderCell(createHeaderCell("Местоположение", font));
            table.addHeaderCell(createHeaderCell("Статус", font));
            table.addHeaderCell(createHeaderCell("Описание", font));
            
            // Данные
            for (Alert alert : alerts) {
                table.addCell(createCell(String.valueOf(alert.getId()), font));
                table.addCell(createCell(String.valueOf(alert.getBusId()), font));
                table.addCell(createCell(translateEventType(alert.getType()), font));
                table.addCell(createCell(alert.getTimestamp().format(DATE_TIME_FORMATTER), font));
                table.addCell(createCell(alert.getLocation(), font));
                table.addCell(createStatusCell(alert.getStatus(), font));
                table.addCell(createCell(truncate(alert.getDescription(), 50), font));
            }
            
            document.add(table);
            
        } catch (Exception e) {
            log.error("Ошибка при добавлении таблицы", e);
            document.add(new Paragraph(title)
                .setBold()
                .setFontSize(SUBHEADER_FONT_SIZE)
                .setMarginBottom(10)
                .setMarginTop(20));
        }
    }
    
    private void addEventTypeStatistics(Document document, List<Alert> alerts, PdfFont font) {
        if (alerts.isEmpty()) return;
        
        try {
            Map<EventType, Long> byType = alerts.stream()
                .collect(Collectors.groupingBy(Alert::getType, Collectors.counting()));
            
            document.add(createRussianParagraph("РАСПРЕДЕЛЕНИЕ ПО ТИПАМ СОБЫТИЙ", font)
                .setBold()
                .setFontSize(SUBHEADER_FONT_SIZE)
                .setMarginBottom(10)
                .setMarginTop(20));
            
            Table typeTable = new Table(UnitValue.createPercentArray(new float[]{2, 1, 1}))
                .useAllAvailableWidth()
                .setMarginBottom(20);
            
            typeTable.addHeaderCell(createHeaderCell("Тип события", font));
            typeTable.addHeaderCell(createHeaderCell("Количество", font));
            typeTable.addHeaderCell(createHeaderCell("Доля, %", font));
            
            long total = alerts.size();
            
            for (Map.Entry<EventType, Long> entry : byType.entrySet()) {
                long count = entry.getValue();
                float percentage = (float) count / total * 100;
                
                typeTable.addCell(createCell(translateEventType(entry.getKey()), font));
                typeTable.addCell(createCell(String.valueOf(count), font));
                typeTable.addCell(createCell(String.format("%.1f%%", percentage), font));
            }
            
            document.add(typeTable);
            
        } catch (Exception e) {
            log.error("Ошибка при добавлении статистики по типам", e);
        }
    }
    
    private void addTopBusesTable(Document document, List<Alert> alerts, PdfFont font) {
        if (alerts.isEmpty()) return;
        
        try {
            Map<Long, Long> busCounts = alerts.stream()
                .collect(Collectors.groupingBy(Alert::getBusId, Collectors.counting()));
            
            List<Map.Entry<Long, Long>> topBuses = busCounts.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .limit(10)
                .collect(Collectors.toList());
            
            if (topBuses.isEmpty()) return;
            
            document.add(createRussianParagraph("ТОП-10 АВТОБУСОВ ПО КОЛИЧЕСТВУ ИНЦИДЕНТОВ", font)
                .setBold()
                .setFontSize(SUBHEADER_FONT_SIZE)
                .setMarginBottom(10)
                .setMarginTop(20));
            
            Table busTable = new Table(UnitValue.createPercentArray(new float[]{1, 2, 1}))
                .useAllAvailableWidth()
                .setMarginBottom(20);
            
            busTable.addHeaderCell(createHeaderCell("№", font));
            busTable.addHeaderCell(createHeaderCell("ID автобуса", font));
            busTable.addHeaderCell(createHeaderCell("Кол-во инцидентов", font));
            
            int rank = 1;
            for (Map.Entry<Long, Long> entry : topBuses) {
                busTable.addCell(createCell(String.valueOf(rank++), font));
                busTable.addCell(createCell(String.valueOf(entry.getKey()), font));
                busTable.addCell(createCell(String.valueOf(entry.getValue()), font));
            }
            
            document.add(busTable);
            
        } catch (Exception e) {
            log.error("Ошибка при добавлении топа автобусов", e);
        }
    }
    
    private void addRecommendationsSection(Document document, List<Alert> alerts, PdfFont font) {
        if (alerts.isEmpty()) return;
        
        try {
            long unresolved = countByStatus(alerts, StatusType.NEW) + 
                             countByStatus(alerts, StatusType.IN_PROGRESS);
            
            if (unresolved > 0) {
                document.add(createRussianParagraph("РЕКОМЕНДАЦИИ", font)
                    .setBold()
                    .setFontSize(SUBHEADER_FONT_SIZE)
                    .setMarginBottom(10)
                    .setMarginTop(20));

                StringBuilder recommendationsText = new StringBuilder();
                recommendationsText.append(String.format("• Требуют внимания: %d нерешённых инцидентов%n", unresolved));
                
                if (countByStatus(alerts, StatusType.NEW) > 10)
                    recommendationsText.append("• Большое количество новых инцидентов - увеличьте количество операторов%n");

                Map<EventType, Long> byType = alerts.stream()
                    .collect(Collectors.groupingBy(Alert::getType, Collectors.counting()));
                
                byType.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .ifPresent(entry -> 
                        recommendationsText.append(String.format("• Наиболее частый тип: %s (%d случаев)%n", 
                            translateEventType(entry.getKey()), entry.getValue()))
                    );
                
                document.add(createRussianParagraph(recommendationsText.toString(), font)
                    .setFontSize(BODY_FONT_SIZE));
            }
            
        } catch (Exception e) {
            log.error("Ошибка при добавлении рекомендаций", e);
        }
    }
    
    private void addNoDataMessage(Document document, PdfFont font) {
        try {
            document.add(createRussianParagraph("За выбранный период инцидентов не обнаружено", font)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(BODY_FONT_SIZE)
                .setFontColor(ColorConstants.GRAY)
                .setMarginTop(20)
                .setMarginBottom(20));
        } catch (Exception e) {
            log.error("Ошибка при добавлении сообщения об отсутствии данных", e);
            document.add(new Paragraph("За выбранный период инцидентов не обнаружено")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(BODY_FONT_SIZE)
                .setFontColor(ColorConstants.GRAY)
                .setMarginTop(20)
                .setMarginBottom(20));
        }
    }
    
    private void addFooter(Document document, PdfFont font) {
        try {
            document.add(createRussianParagraph("\n\n", font));
            
            Div footerLine = new Div();
            footerLine.setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f));
            footerLine.setHeight(0.5f);
            document.add(footerLine);
            
            document.add(createRussianParagraph(
                    String.format("Отчет сгенерирован: %s | Система мониторинга инцидентов v1.0",
                        LocalDateTime.now().format(DATE_TIME_FORMATTER)), font)
                .setFontSize(SMALL_FONT_SIZE)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.DARK_GRAY)
                .setMarginTop(5));
                
        } catch (Exception e) {
            log.error("Ошибка при добавлении подвала", e);
            document.add(new Paragraph("\n\n"));
            
            Div footerLine = new Div();
            footerLine.setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f));
            footerLine.setHeight(0.5f);
            document.add(footerLine);
        }
    }
    
    private Cell createStatCell(String text, boolean isLabel, PdfFont font) {
        try {
            Cell cell = new Cell()
                .add(createRussianParagraph(text, font)
                    .setFontSize(BODY_FONT_SIZE)
                    .setPadding(5));
            
            if (isLabel) {
                cell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
                cell.setBold();
            }
            
            return cell;
        } catch (Exception e) {
            log.error("Ошибка создания ячейки статистики", e);
            Cell cell = new Cell()
                .add(new Paragraph(text)
                    .setFontSize(BODY_FONT_SIZE)
                    .setPadding(5));
            
            if (isLabel) {
                cell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
                cell.setBold();
            }
            
            return cell;
        }
    }
    
    private Cell createHeaderCell(String text, PdfFont font) {
        try {
            return new Cell()
                .add(createRussianParagraph(text, font)
                    .setBold()
                    .setFontSize(BODY_FONT_SIZE))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setPadding(5)
                .setTextAlignment(TextAlignment.CENTER);
        } catch (Exception e) {
            log.error("Ошибка создания заголовочной ячейки", e);
            return new Cell()
                .add(new Paragraph(text)
                    .setBold()
                    .setFontSize(BODY_FONT_SIZE))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setPadding(5)
                .setTextAlignment(TextAlignment.CENTER);
        }
    }
    
    private Cell createCell(String text, PdfFont font) {
        try {
            return new Cell()
                .add(createRussianParagraph(text, font)
                    .setFontSize(BODY_FONT_SIZE))
                .setPadding(5);
        } catch (Exception e) {
            log.error("Ошибка создания ячейки", e);
            return new Cell()
                .add(new Paragraph(text)
                    .setFontSize(BODY_FONT_SIZE))
                .setPadding(5);
        }
    }
    
    private Cell createStatusCell(StatusType status, PdfFont font) {
        try {
            Cell cell = new Cell()
                .add(createRussianParagraph(translateStatus(status), font)
                    .setFontSize(BODY_FONT_SIZE))
                .setPadding(5)
                .setTextAlignment(TextAlignment.CENTER);
            
            switch (status) {
                case NEW -> cell.setBackgroundColor(ColorConstants.YELLOW);
                case IN_PROGRESS -> {
                    cell.setBackgroundColor(ColorConstants.BLUE);
                    cell.setFontColor(ColorConstants.WHITE);
                }
                case RESOLVED -> {
                    cell.setBackgroundColor(ColorConstants.GREEN);
                    cell.setFontColor(ColorConstants.WHITE);
                }
            }
            
            return cell;
        } catch (Exception e) {
            log.error("Ошибка создания ячейки статуса", e);
            Cell cell = new Cell()
                .add(new Paragraph(translateStatus(status))
                    .setFontSize(BODY_FONT_SIZE))
                .setPadding(5)
                .setTextAlignment(TextAlignment.CENTER);
            
            switch (status) {
                case NEW -> cell.setBackgroundColor(ColorConstants.YELLOW);
                case IN_PROGRESS -> {
                    cell.setBackgroundColor(ColorConstants.BLUE);
                    cell.setFontColor(ColorConstants.WHITE);
                }
                case RESOLVED -> {
                    cell.setBackgroundColor(ColorConstants.GREEN);
                    cell.setFontColor(ColorConstants.WHITE);
                }
            }
            
            return cell;
        }
    }
    
    private long countByStatus(List<Alert> alerts, StatusType status) {
        return alerts.stream()
            .filter(alert -> alert.getStatus() == status)
            .count();
    }
    
    private String translateEventType(EventType type) {
        return switch (type) {
            case ACCIDENT -> "Авария";
            case HARD_BRAKING -> "Резкое торможение";
            case BUTTON -> "Нажатие кнопки";
        };
    }
    
    private String translateStatus(StatusType status) {
        return switch (status) {
            case NEW -> "Новый";
            case IN_PROGRESS -> "В работе";
            case RESOLVED -> "Решен";
        };
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
    
    public static class ReportGenerationException extends RuntimeException {
        public ReportGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}