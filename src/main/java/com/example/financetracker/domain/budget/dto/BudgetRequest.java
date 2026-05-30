package com.example.financetracker.domain.budget.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BudgetRequest {

    @NotBlank
    private String name;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal limitAmount;

    @NotNull
    @Min(1) @Max(12)
    private Integer month;

    @NotNull
    @Min(2000)
    private Integer year;

    private Long categoryId;
}
