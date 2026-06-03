package com.example.financetracker.domain.transaction;

import com.example.financetracker.common.dto.PageResponse;
import com.example.financetracker.common.exception.ResourceNotFoundException;
import com.example.financetracker.domain.account.AccountRepository;
import com.example.financetracker.domain.auth.User;
import com.example.financetracker.domain.auth.UserRepository;
import com.example.financetracker.domain.category.Category;
import com.example.financetracker.domain.category.CategoryRepository;
import com.example.financetracker.domain.transaction.dto.NoteRequest;
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
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public PageResponse<TransactionResponse> getAll(TransactionType type, Long categoryId,
            LocalDate startDate, LocalDate endDate, BigDecimal minAmount, BigDecimal maxAmount,
            String search, Boolean recurring, Long accountId, Long destinationAccountId, Pageable pageable) {
        User user = currentUser();
        Specification<Transaction> spec = TransactionSpec.forUser(user);
        if (type != null)                  spec = spec.and(TransactionSpec.hasType(type));
        if (categoryId != null)            spec = spec.and(TransactionSpec.hasCategoryId(categoryId));
        if (startDate != null)             spec = spec.and(TransactionSpec.afterOrOn(startDate));
        if (endDate != null)               spec = spec.and(TransactionSpec.beforeOrOn(endDate));
        if (minAmount != null)             spec = spec.and(TransactionSpec.amountMin(minAmount));
        if (maxAmount != null)             spec = spec.and(TransactionSpec.amountMax(maxAmount));
        if (search != null && !search.isBlank()) spec = spec.and(TransactionSpec.searchText(search));
        if (recurring != null)             spec = spec.and(TransactionSpec.isRecurring(recurring));
        if (accountId != null)             spec = spec.and(TransactionSpec.hasAccountId(accountId));
        if (destinationAccountId != null)  spec = spec.and(TransactionSpec.hasDestinationAccountId(destinationAccountId));
        return PageResponse.from(transactionRepository.findAll(spec, pageable).map(TransactionResponse::from));
    }

    public TransactionResponse create(TransactionRequest request) {
        User user = currentUser();
        Category category = resolveCategory(request, user);
        Transaction transaction = Transaction.builder()
                .type(request.getType())
                .amount(request.getAmount())
                .description(request.getDescription())
                .transactionDate(request.getTransactionDate())
                .notes(request.getNotes())
                .accountId(request.getAccountId())
                .destinationAccountId(request.getDestinationAccountId())
                .recurring(request.getRecurring() != null ? request.getRecurring() : false)
                .recurrenceFrequency(request.getRecurrenceFrequency())
                .category(category)
                .user(user)
                .build();
        Transaction saved = transactionRepository.save(transaction);
        applyBalanceEffect(request.getType(), request.getAccountId(), request.getDestinationAccountId(), request.getAmount(), false);
        return TransactionResponse.from(saved);
    }

    public TransactionResponse update(Long id, TransactionRequest request) {
        User user = currentUser();
        Transaction transaction = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
        Category category = resolveCategory(request, user);

        // Only adjust balance if not hidden (hidden transactions don't affect balance)
        if (!Boolean.TRUE.equals(transaction.getHidden())) {
            applyBalanceEffect(transaction.getType(), transaction.getAccountId(), transaction.getDestinationAccountId(), transaction.getAmount(), true);
            applyBalanceEffect(request.getType(), request.getAccountId(), request.getDestinationAccountId(), request.getAmount(), false);
        }

        transaction.setType(request.getType());
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setNotes(request.getNotes());
        transaction.setAccountId(request.getAccountId());
        transaction.setDestinationAccountId(request.getDestinationAccountId());
        transaction.setRecurring(request.getRecurring() != null ? request.getRecurring() : false);
        transaction.setRecurrenceFrequency(request.getRecurrenceFrequency());
        transaction.setCategory(category);
        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    public TransactionResponse updateNote(Long id, NoteRequest request) {
        Transaction transaction = transactionRepository.findByIdAndUser(id, currentUser())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
        transaction.setNotes(request.getNotes());
        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    public TransactionResponse toggleHidden(Long id) {
        Transaction transaction = transactionRepository.findByIdAndUser(id, currentUser())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
        boolean nowHidden = !Boolean.TRUE.equals(transaction.getHidden());
        transaction.setHidden(nowHidden);
        // Reverse balance when hiding, reapply when un-hiding
        applyBalanceEffect(transaction.getType(), transaction.getAccountId(), transaction.getDestinationAccountId(), transaction.getAmount(), nowHidden);
        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    public void delete(Long id) {
        Transaction transaction = transactionRepository.findByIdAndUser(id, currentUser())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
        // Only reverse balance if not already hidden (balance was already reversed on hide)
        if (!Boolean.TRUE.equals(transaction.getHidden())) {
            applyBalanceEffect(transaction.getType(), transaction.getAccountId(), transaction.getDestinationAccountId(), transaction.getAmount(), true);
        }
        transactionRepository.delete(transaction);
    }

    private Category resolveCategory(TransactionRequest request, User user) {
        if (request.getType() == TransactionType.TRANSFER || request.getCategoryId() == null) return null;
        return categoryRepository.findByIdAndUserOrDefault(request.getCategoryId(), user)
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
    }

    /**
     * Applies or reverses the balance effect of a transaction.
     * For TRANSFER: source loses money, destination gains money (reversed when reverse=true).
     * For INCOME/EXPENSE: standard XOR logic.
     */
    private void applyBalanceEffect(TransactionType type, Long accountId, Long destinationAccountId, BigDecimal amount, boolean reverse) {
        if (amount == null) return;
        if (type == TransactionType.TRANSFER) {
            adjustSingleBalance(accountId, amount, reverse);           // source: add back when reversing, subtract when not
            adjustSingleBalance(destinationAccountId, amount, !reverse); // dest: subtract when reversing, add when not
        } else {
            boolean isIncome = type == TransactionType.INCOME;
            adjustSingleBalance(accountId, amount, isIncome != reverse);
        }
    }

    private void adjustSingleBalance(Long accountId, BigDecimal amount, boolean add) {
        if (accountId == null) return;
        accountRepository.findById(accountId).ifPresent(account -> {
            BigDecimal current = account.getCurrentBalance() != null ? account.getCurrentBalance() : BigDecimal.ZERO;
            account.setCurrentBalance(add ? current.add(amount) : current.subtract(amount));
            accountRepository.save(account);
        });
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", 0L));
    }
}
