package com.example.financetracker.domain.goal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class GoalRequest {

    @NotBlank
    private String name;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal targetAmount;

    @DecimalMin("0.00")
    private BigDecimal savedAmount;

    private LocalDate targetDate;
}
