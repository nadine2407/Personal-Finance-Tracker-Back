package com.example.financetracker.domain.budget.dto;

import com.example.financetracker.domain.budget.Budget;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BudgetResponse {

    private Long id;
    private String name;
    private BigDecimal limitAmount;
    private Integer month;
    private Integer year;
    private Long categoryId;
    private String categoryName;

    public static BudgetResponse from(Budget budget) {
        BudgetResponse dto = new BudgetResponse();
        dto.setId(budget.getId());
        dto.setName(budget.getName());
        dto.setLimitAmount(budget.getLimitAmount());
        dto.setMonth(budget.getMonth());
        dto.setYear(budget.getYear());
        if (budget.getCategory() != null) {
            dto.setCategoryId(budget.getCategory().getId());
            dto.setCategoryName(budget.getCategory().getName());
        }
        return dto;
    }
}
