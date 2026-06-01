package com.example.financetracker.domain.account.dto;

import com.example.financetracker.domain.account.Account;
import com.example.financetracker.domain.account.AccountType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountResponse {

    private Long id;
    private String name;
    private AccountType type;
    private BigDecimal initialBalance;
    private BigDecimal currentBalance;
    private BigDecimal goalAllocatedAmount;
    private BigDecimal effectiveBalance;
    private String color;
    private String icon;

    public static AccountResponse from(Account account, BigDecimal goalAllocated) {
        AccountResponse dto = new AccountResponse();
        dto.setId(account.getId());
        dto.setName(account.getName());
        dto.setType(account.getType());
        dto.setInitialBalance(account.getInitialBalance());
        BigDecimal balance = account.getCurrentBalance() != null
                ? account.getCurrentBalance()
                : account.getInitialBalance();
        dto.setCurrentBalance(balance);
        BigDecimal allocated = goalAllocated != null ? goalAllocated : BigDecimal.ZERO;
        dto.setGoalAllocatedAmount(allocated);
        dto.setEffectiveBalance(balance.subtract(allocated));
        dto.setColor(account.getColor());
        dto.setIcon(account.getIcon());
        return dto;
    }
}
