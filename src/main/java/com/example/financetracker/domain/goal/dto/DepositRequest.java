package com.example.financetracker.domain.goal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepositRequest {
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    private Long accountId;
}
