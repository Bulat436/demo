package com.example.demo.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "bus_id", nullable = false)
    private Long busId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType type;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    private String location;
    
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
