package com.example.financetracker.domain.transaction.dto;

import com.example.financetracker.domain.transaction.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionRequest {

    @NotNull
    private TransactionType type;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    private String description;

    @NotNull
    private LocalDate transactionDate;

    private Long categoryId;

    private String notes;
    private Long accountId;
    private Long destinationAccountId;
    private Boolean recurring;
    private String recurrenceFrequency;
}
