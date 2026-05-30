package com.example.financetracker.domain.budget.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class BudgetPageSummary {
    private int year;
    private int month;
    private BigDecimal totalBudget;
    private BigDecimal totalSpent;
    private BigDecimal remaining;
    private BigDecimal overspend;
    private int warnCount;
    private int overCount;
    private List<BudgetStatusItem> items;
}
