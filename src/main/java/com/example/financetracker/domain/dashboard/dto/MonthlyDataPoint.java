package com.example.financetracker.domain.dashboard.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonthlyDataPoint {
    private int month;
    private double income;
    private double expenses;
}
