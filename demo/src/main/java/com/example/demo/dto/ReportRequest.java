package com.example.demo.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReportRequest {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private ReportType reportType;
    private List<Long> busIds;
    private List<String> statuses;
    private boolean includeCharts = true;
    private boolean includeSummary = true;
    
    public enum ReportType {
        DAILY,
        WEEKLY, 
        MONTHLY,
        CUSTOM
    }
}