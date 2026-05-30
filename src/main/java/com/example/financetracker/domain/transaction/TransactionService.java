package com.example.financetracker.domain.transaction;

import com.example.financetracker.common.exception.ResourceNotFoundException;
import com.example.financetracker.domain.auth.User;
import com.example.financetracker.domain.auth.UserRepository;
import com.example.financetracker.domain.category.Category;
import com.example.financetracker.domain.category.CategoryRepository;
import com.example.financetracker.domain.transaction.dto.TransactionRequest;
import com.example.financetracker.domain.transaction.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public List<TransactionResponse> getAll() {
        return transactionRepository.findByUserOrderByDateDesc(currentUser()).stream()
                .map(TransactionResponse::from)
                .toList();
    }

    public TransactionResponse create(TransactionRequest request) {
        User user = currentUser();
        Category category = categoryRepository.findByIdAndUser(request.getCategoryId(), user)
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
        Transaction transaction = Transaction.builder()
                .amount(request.getAmount())
                .description(request.getDescription())
                .date(request.getDate())
                .category(category)
                .user(user)
                .build();
        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    public TransactionResponse update(Long id, TransactionRequest request) {
        User user = currentUser();
        Transaction transaction = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
        Category category = categoryRepository.findByIdAndUser(request.getCategoryId(), user)
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setDate(request.getDate());
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
