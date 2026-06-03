package com.example.financetracker.domain.goal.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DebitRequest {
    @NotNull
    private Long checkingAccountId;
}
