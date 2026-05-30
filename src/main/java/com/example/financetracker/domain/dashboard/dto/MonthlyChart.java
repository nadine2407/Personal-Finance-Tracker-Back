package com.example.financetracker.domain.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MonthlyChart {
    private int year;
    private List<MonthlyDataPoint> data;
}
