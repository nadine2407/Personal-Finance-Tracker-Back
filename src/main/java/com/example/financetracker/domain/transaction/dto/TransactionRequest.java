package com.example.financetracker.domain.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionRequest {

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    private String description;

    @NotNull
    private LocalDate date;

    @NotNull
    private Long categoryId;
}
