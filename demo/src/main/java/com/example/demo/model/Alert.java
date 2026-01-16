package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    private String filePath;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "Bus ID не может быть пустым")
    @Column(name = "bus_id", nullable = false)
    private Long busId;
    
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_id", insertable = false, updatable = false)
    private Bus bus;
    
    @NotNull(message = "Тип инцидента обязателен")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType type;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @NotBlank(message = "Местоположение не может быть пустым")
    private String location;
    
    @NotBlank(message = "Описание не может быть пустым")
    private String description;
    
    @Enumerated(EnumType.STRING)
    private StatusType status;
    
    @Column(name = "assigned_to_user_id")
    private Long assignedToUserId;
    
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (status == null) {
            status = StatusType.NEW;
        }
    }
}