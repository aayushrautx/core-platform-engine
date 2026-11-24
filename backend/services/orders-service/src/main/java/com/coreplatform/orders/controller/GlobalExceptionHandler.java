package com.coreplatform.orders.controller;

import com.coreplatform.authorization.aspect.AccessDeniedException;
import com.coreplatform.authorization.guard.TenantMismatchException;
import com.coreplatform.orders.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(TenantMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTenantMismatch(TenantMismatchException e) {
        return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(OrderService.OrderNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(OrderService.OrderNotFoundException e) {
        return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
    }
}
