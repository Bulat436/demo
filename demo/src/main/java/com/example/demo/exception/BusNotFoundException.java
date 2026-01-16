package com.example.demo.exception;

public class BusNotFoundException extends RuntimeException {
    public BusNotFoundException(Long id) {
        super("Автобус с ID " + id + " не найден");
    }
    
    public BusNotFoundException(String message) {
        super(message);
    }
}