package com.example.financetracker.domain.goal.dto;

import com.example.financetracker.domain.goal.Goal;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
public class GoalResponse {

    private Long id;
    private String name;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private LocalDate deadline;
    private String description;
    private boolean completed;
    private double progressPercent;
    private BigDecimal remainingAmount;
    private Long linkedAccountId;
    private BigDecimal linkedAccountAmount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static GoalResponse from(Goal goal) {
        GoalResponse dto = new GoalResponse();
        dto.setId(goal.getId());
        dto.setName(goal.getName());
        dto.setTargetAmount(goal.getTargetAmount());
        dto.setCurrentAmount(goal.getCurrentAmount());
        dto.setDeadline(goal.getDeadline());
        dto.setDescription(goal.getDescription());
        dto.setLinkedAccountId(goal.getLinkedAccount() != null ? goal.getLinkedAccount().getId() : null);
        dto.setLinkedAccountAmount(goal.getLinkedAccountAmount() != null ? goal.getLinkedAccountAmount() : BigDecimal.ZERO);
        dto.setCreatedAt(goal.getCreatedAt());
        dto.setUpdatedAt(goal.getUpdatedAt());

        BigDecimal current = goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO;
        BigDecimal target = goal.getTargetAmount();

        dto.setCompleted(current.compareTo(target) >= 0);
        dto.setRemainingAmount(target.subtract(current).max(BigDecimal.ZERO));

        if (target.compareTo(BigDecimal.ZERO) > 0) {
            double pct = current.multiply(BigDecimal.valueOf(100))
                    .divide(target, 2, RoundingMode.HALF_UP)
                    .doubleValue();
            dto.setProgressPercent(Math.min(pct, 100.0));
        } else {
            dto.setProgressPercent(0.0);
        }
        return dto;
    }
}
