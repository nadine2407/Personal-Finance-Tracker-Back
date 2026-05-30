package com.example.financetracker.domain.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardSummary {
    private int year;
    private int month;
    private double totalIncome;
    private double totalExpenses;
    private double netSavings;
    private List<CategoryBreakdown> categoryBreakdown;
}
