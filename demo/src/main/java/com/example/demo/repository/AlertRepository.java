package com.example.demo.repository;

import com.example.demo.enums.StatusType;
import com.example.demo.model.Alert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long>, JpaSpecificationExecutor<Alert> {

    List<Alert> findByStatus(StatusType status);
    List<Alert> findByBusId(Long busId);
    List<Alert> findByAssignedToUserId(Long userId);
    List<Alert> findByTimestampBetween(LocalDateTime startDate, LocalDateTime endDate);
}