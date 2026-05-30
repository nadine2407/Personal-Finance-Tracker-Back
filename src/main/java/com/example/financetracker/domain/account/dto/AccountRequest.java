package com.example.financetracker.domain.account.dto;

import com.example.financetracker.domain.account.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountRequest {

    @NotBlank
    private String name;

    @NotNull
    private AccountType type;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal initialBalance;

    private String color;
    private String icon;
}
