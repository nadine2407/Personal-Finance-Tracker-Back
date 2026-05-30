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
    private BigDecimal balance;

    public static AccountResponse from(Account account) {
        AccountResponse dto = new AccountResponse();
        dto.setId(account.getId());
        dto.setName(account.getName());
        dto.setType(account.getType());
        dto.setBalance(account.getBalance());
        return dto;
    }
}
