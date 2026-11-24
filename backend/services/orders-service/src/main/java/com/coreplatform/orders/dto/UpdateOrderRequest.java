package com.coreplatform.orders.dto;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record UpdateOrderRequest(
        String description,
        @DecimalMin("0.01") BigDecimal amount,
        String status
) {}
