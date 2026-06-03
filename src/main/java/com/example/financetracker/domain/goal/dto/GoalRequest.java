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

    private LocalDate deadline;
    private String description;

    /** ID du compte épargne auquel cet objectif est rattaché (obligatoire). */
    @NotNull
    private Long linkedAccountId;

    /** Montant initialement alloué depuis le compte épargne (0 si non renseigné). */
    @DecimalMin("0.00")
    private BigDecimal allocatedAmount;
}
