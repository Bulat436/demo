package com.example.demo.specification;

import org.springframework.data.jpa.domain.Specification;

import com.example.demo.model.Alert;
import com.example.demo.model.StatusType;



public class AlertSpecification {
    public static Specification<Alert> hasStatus(StatusType status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return null;
            }
            return criteriaBuilder.equal(root.get("status"), status);
        };
    }
    public static Specification<Alert> hasBusId(Long busId) {

        return (root, query, criteriaBuilder) -> {
            if (busId == null) {
                return null;
            }
        return criteriaBuilder.equal(root.get("busId"), busId);
        };
    }

     public static Specification<Alert> locationContains(String location) {
        return (root, query, criteriaBuilder) -> {
            if (location == null || location.trim().isEmpty()) {
                return null;
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get("location")), 
                "%" + location.toLowerCase().trim() + "%"
            );
        };
    }
    public static Specification<Alert> filter(StatusType status, Long busId, String location) {
        return Specification.allOf(
            hasStatus(status),
            hasBusId(busId),
            locationContains(location)
        );
    }
}