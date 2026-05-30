package com.example.financetracker.domain.transaction.dto;

import com.example.financetracker.domain.category.CategoryType;
import com.example.financetracker.domain.transaction.Transaction;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionResponse {

    private Long id;
    private BigDecimal amount;
    private String description;
    private LocalDate date;
    private Long categoryId;
    private String categoryName;
    private CategoryType categoryType;

    public static TransactionResponse from(Transaction t) {
        TransactionResponse dto = new TransactionResponse();
        dto.setId(t.getId());
        dto.setAmount(t.getAmount());
        dto.setDescription(t.getDescription());
        dto.setDate(t.getDate());
        dto.setCategoryId(t.getCategory().getId());
        dto.setCategoryName(t.getCategory().getName());
        dto.setCategoryType(t.getCategory().getType());
        return dto;
    }
}
