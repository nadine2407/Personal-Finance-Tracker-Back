package com.example.financetracker.domain.goal.dto;

import com.example.financetracker.domain.goal.Goal;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class GoalResponse {

    private Long id;
    private String name;
    private BigDecimal targetAmount;
    private BigDecimal savedAmount;
    private LocalDate targetDate;

    public static GoalResponse from(Goal goal) {
        GoalResponse dto = new GoalResponse();
        dto.setId(goal.getId());
        dto.setName(goal.getName());
        dto.setTargetAmount(goal.getTargetAmount());
        dto.setSavedAmount(goal.getSavedAmount());
        dto.setTargetDate(goal.getTargetDate());
        return dto;
    }
}
