package com.example.financetracker.domain.transaction.dto;

import com.example.financetracker.domain.transaction.Transaction;
import com.example.financetracker.domain.transaction.TransactionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
public class TransactionResponse {

    private Long id;
    private TransactionType type;
    private BigDecimal amount;
    private String description;
    private LocalDate transactionDate;
    private CategoryInfo category;
    private String notes;
    private Long accountId;
    private String accountName;
    private Boolean recurring;
    private String recurrenceFrequency;
    private Boolean hidden;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @Data
    public static class CategoryInfo {
        private Long id;
        private String name;
        private String icon;
        private String color;
    }

    public static TransactionResponse from(Transaction t) {
        TransactionResponse dto = new TransactionResponse();
        dto.setId(t.getId());
        dto.setType(t.getType());
        dto.setAmount(t.getAmount());
        dto.setDescription(t.getDescription());
        dto.setTransactionDate(t.getTransactionDate());
        dto.setNotes(t.getNotes());
        dto.setAccountId(t.getAccountId());
        dto.setRecurring(t.getRecurring());
        dto.setRecurrenceFrequency(t.getRecurrenceFrequency());
        dto.setHidden(Boolean.TRUE.equals(t.getHidden()));
        dto.setCreatedAt(t.getCreatedAt());
        dto.setUpdatedAt(t.getUpdatedAt());

        if (t.getCategory() != null) {
            CategoryInfo cat = new CategoryInfo();
            cat.setId(t.getCategory().getId());
            cat.setName(t.getCategory().getName());
            cat.setIcon(t.getCategory().getIcon());
            cat.setColor(t.getCategory().getColor());
            dto.setCategory(cat);
        }
        return dto;
    }
}
