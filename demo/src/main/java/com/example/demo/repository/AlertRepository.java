package com.example.demo.repository;

import com.example.demo.model.Alert;
import com.example.demo.model.StatusType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByStatus(StatusType status);
    
    List<Alert> findByBusId(Long busId);
    List<Alert> findByAssignedToUserId(Long userId);
}