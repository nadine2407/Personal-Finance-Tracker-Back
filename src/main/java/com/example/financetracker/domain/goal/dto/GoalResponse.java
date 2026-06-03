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
    private BigDecimal allocatedAmount;
    private LocalDate deadline;
    private String description;
    private boolean completed;
    private boolean debited;
    private LocalDate debitedAt;
    private double progressPercent;
    private BigDecimal remainingAmount;
    private Long linkedAccountId;
    private String linkedAccountName;
    private Integer priority;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static GoalResponse from(Goal goal) {
        GoalResponse dto = new GoalResponse();
        dto.setId(goal.getId());
        dto.setName(goal.getName());
        dto.setTargetAmount(goal.getTargetAmount());
        dto.setDeadline(goal.getDeadline());
        dto.setDescription(goal.getDescription());
        dto.setPriority(goal.getPriority());
        dto.setCreatedAt(goal.getCreatedAt());
        dto.setUpdatedAt(goal.getUpdatedAt());

        // Allocated amount = what's concretely reserved on the linked account
        BigDecimal allocated = goal.getLinkedAccountAmount() != null
                ? goal.getLinkedAccountAmount()
                : BigDecimal.ZERO;
        dto.setAllocatedAmount(allocated);

        if (goal.getLinkedAccount() != null) {
            dto.setLinkedAccountId(goal.getLinkedAccount().getId());
            dto.setLinkedAccountName(goal.getLinkedAccount().getName());
        }

        dto.setDebited(goal.isDebited());
        dto.setDebitedAt(goal.getDebitedAt());

        BigDecimal target = goal.getTargetAmount();
        dto.setCompleted(allocated.compareTo(target) >= 0 || goal.isDebited());
        dto.setRemainingAmount(target.subtract(allocated).max(BigDecimal.ZERO));

        if (target.compareTo(BigDecimal.ZERO) > 0) {
            double pct = allocated.multiply(BigDecimal.valueOf(100))
                    .divide(target, 2, RoundingMode.HALF_UP)
                    .doubleValue();
            dto.setProgressPercent(Math.min(pct, 100.0));
        } else {
            dto.setProgressPercent(0.0);
        }
        return dto;
    }
}
