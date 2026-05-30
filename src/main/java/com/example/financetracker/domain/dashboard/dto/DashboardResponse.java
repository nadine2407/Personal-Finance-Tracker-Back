package com.example.financetracker.domain.dashboard.dto;

import com.example.financetracker.domain.transaction.dto.TransactionResponse;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardResponse {
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal balance;
    private long totalTransactions;
    private long totalGoals;
    private long totalBudgets;
    private List<TransactionResponse> recentTransactions;
}
