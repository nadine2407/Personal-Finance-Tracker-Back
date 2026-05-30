package com.example.financetracker.domain.budget.dto;

import com.example.financetracker.domain.budget.Budget;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BudgetResponse {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private String categoryColor;
    private String categoryIcon;
    private BigDecimal amount;
    private Integer month;
    private Integer year;

    public static BudgetResponse from(Budget budget) {
        BudgetResponse dto = new BudgetResponse();
        dto.setId(budget.getId());
        dto.setAmount(budget.getAmount());
        dto.setMonth(budget.getMonth());
        dto.setYear(budget.getYear());
        if (budget.getCategory() != null) {
            dto.setCategoryId(budget.getCategory().getId());
            dto.setCategoryName(budget.getCategory().getName());
            dto.setCategoryColor(budget.getCategory().getColor());
            dto.setCategoryIcon(budget.getCategory().getIcon());
        }
        return dto;
    }
}
