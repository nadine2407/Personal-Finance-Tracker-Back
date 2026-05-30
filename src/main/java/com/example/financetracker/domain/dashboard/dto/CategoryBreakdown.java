package com.example.financetracker.domain.dashboard.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryBreakdown {
    private String name;
    private String color;
    private String icon;
    private double amount;
    private double percentage;
}
