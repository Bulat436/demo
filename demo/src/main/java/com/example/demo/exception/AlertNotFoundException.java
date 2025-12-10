package com.example.demo.exception;

public class AlertNotFoundException extends RuntimeException {
    public AlertNotFoundException(Long id) {
        super("Инцидент с ID " + id + " не найден");
    }
}