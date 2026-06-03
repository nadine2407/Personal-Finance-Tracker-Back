package com.example.financetracker.domain.transaction;

import com.example.financetracker.domain.auth.User;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionSpec {

    public static Specification<Transaction> forUser(User user) {
        return (root, query, cb) -> cb.equal(root.get("user"), user);
    }

    public static Specification<Transaction> hasType(TransactionType type) {
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<Transaction> hasCategoryId(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Transaction> afterOrOn(LocalDate startDate) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("transactionDate"), startDate);
    }

    public static Specification<Transaction> beforeOrOn(LocalDate endDate) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("transactionDate"), endDate);
    }

    public static Specification<Transaction> amountMin(BigDecimal min) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("amount"), min);
    }

    public static Specification<Transaction> amountMax(BigDecimal max) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("amount"), max);
    }

    public static Specification<Transaction> searchText(String text) {
        String pattern = "%" + text.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("description")), pattern),
                cb.like(cb.lower(root.get("notes")), pattern)
        );
    }

    public static Specification<Transaction> isRecurring(Boolean recurring) {
        return (root, query, cb) -> cb.equal(root.get("recurring"), recurring);
    }

    public static Specification<Transaction> hasAccountId(Long accountId) {
        return (root, query, cb) -> cb.equal(root.get("accountId"), accountId);
    }

    public static Specification<Transaction> hasDestinationAccountId(Long accountId) {
        return (root, query, cb) -> cb.equal(root.get("destinationAccountId"), accountId);
    }

}
