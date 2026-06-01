package com.example.financetracker.domain.transaction;

import com.example.financetracker.common.dto.PageResponse;
import com.example.financetracker.common.exception.ResourceNotFoundException;
import com.example.financetracker.domain.auth.User;
import com.example.financetracker.domain.auth.UserRepository;
import com.example.financetracker.domain.category.Category;
import com.example.financetracker.domain.category.CategoryRepository;
import com.example.financetracker.domain.transaction.dto.TransactionRequest;
import com.example.financetracker.domain.transaction.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@Transactional
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public PageResponse<TransactionResponse> getAll(TransactionType type, Long categoryId,
            LocalDate startDate, LocalDate endDate, BigDecimal minAmount, BigDecimal maxAmount,
            String search, Pageable pageable) {
        User user = currentUser();
        Specification<Transaction> spec = TransactionSpec.forUser(user);
        if (type != null)       spec = spec.and(TransactionSpec.hasType(type));
        if (categoryId != null) spec = spec.and(TransactionSpec.hasCategoryId(categoryId));
        if (startDate != null)  spec = spec.and(TransactionSpec.afterOrOn(startDate));
        if (endDate != null)    spec = spec.and(TransactionSpec.beforeOrOn(endDate));
        if (minAmount != null)  spec = spec.and(TransactionSpec.amountMin(minAmount));
        if (maxAmount != null)  spec = spec.and(TransactionSpec.amountMax(maxAmount));
        if (search != null && !search.isBlank()) spec = spec.and(TransactionSpec.searchText(search));
        return PageResponse.from(transactionRepository.findAll(spec, pageable).map(TransactionResponse::from));
    }

    public TransactionResponse create(TransactionRequest request) {
        User user = currentUser();
        Category category = categoryRepository.findByIdAndUserOrDefault(request.getCategoryId(), user)
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
        Transaction transaction = Transaction.builder()
                .type(request.getType())
                .amount(request.getAmount())
                .description(request.getDescription())
                .transactionDate(request.getTransactionDate())
                .notes(request.getNotes())
                .accountId(request.getAccountId())
                .recurring(request.getRecurring() != null ? request.getRecurring() : false)
                .recurrenceFrequency(request.getRecurrenceFrequency())
                .split(request.getSplit() != null ? request.getSplit() : false)
                .category(category)
                .user(user)
                .build();
        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    public TransactionResponse update(Long id, TransactionRequest request) {
        User user = currentUser();
        Transaction transaction = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
        Category category = categoryRepository.findByIdAndUserOrDefault(request.getCategoryId(), user)
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
        transaction.setType(request.getType());
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setNotes(request.getNotes());
        transaction.setAccountId(request.getAccountId());
        transaction.setRecurring(request.getRecurring() != null ? request.getRecurring() : false);
        transaction.setRecurrenceFrequency(request.getRecurrenceFrequency());
        transaction.setSplit(request.getSplit() != null ? request.getSplit() : false);
        transaction.setCategory(category);
        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    public void delete(Long id) {
        Transaction transaction = transactionRepository.findByIdAndUser(id, currentUser())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
        transactionRepository.delete(transaction);
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", 0L));
    }
}
