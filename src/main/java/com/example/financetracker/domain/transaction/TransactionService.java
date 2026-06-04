package com.example.financetracker.domain.transaction;

import com.example.financetracker.common.dto.PageResponse;
import com.example.financetracker.common.exception.ResourceNotFoundException;
import com.example.financetracker.domain.account.AccountRepository;
import com.example.financetracker.domain.auth.User;
import com.example.financetracker.domain.auth.UserRepository;
import com.example.financetracker.domain.category.Category;
import com.example.financetracker.domain.category.CategoryRepository;
import com.example.financetracker.domain.goal.GoalService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final GoalService goalService;

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
        boolean isRecurring = Boolean.TRUE.equals(request.getRecurring()) && request.getRecurrenceFrequency() != null;

        if (isRecurring) {
            return createRecurringSeries(request, category, user);
        }

        Transaction transaction = buildTransaction(request, category, user, null, null, null);
        Transaction saved = transactionRepository.save(transaction);
        applyBalanceEffect(request.getType(), request.getAccountId(), request.getDestinationAccountId(), request.getAmount(), false);
        rebalanceAccounts(request.getAccountId(), request.getDestinationAccountId());
        return TransactionResponse.from(saved);
    }

    private TransactionResponse createRecurringSeries(TransactionRequest request, Category category, User user) {
        LocalDate start = request.getTransactionDate();
        LocalDate maxEnd = start.plusYears(1);
        LocalDate end = request.getRecurrenceEndDate() != null
                ? (request.getRecurrenceEndDate().isAfter(maxEnd) ? maxEnd : request.getRecurrenceEndDate())
                : maxEnd;

        String groupId = UUID.randomUUID().toString();
        List<LocalDate> dates = computeOccurrenceDates(start, request.getRecurrenceFrequency(), end);
        LocalDate today = LocalDate.now();

        Transaction first = null;
        for (LocalDate date : dates) {
            Transaction tx = buildTransaction(request, category, user, groupId, end, date);
            Transaction saved = transactionRepository.save(tx);
            // Apply balance only for past/current occurrences
            if (!date.isAfter(today)) {
                applyBalanceEffect(request.getType(), request.getAccountId(), request.getDestinationAccountId(), request.getAmount(), false);
            }
            if (first == null) first = saved;
        }
        rebalanceAccounts(request.getAccountId(), request.getDestinationAccountId());
        return TransactionResponse.from(first);
    }

    private Transaction buildTransaction(TransactionRequest request, Category category, User user,
                                         String groupId, LocalDate endDate, LocalDate date) {
        return Transaction.builder()
                .type(request.getType())
                .amount(request.getAmount())
                .description(request.getDescription())
                .transactionDate(date != null ? date : request.getTransactionDate())
                .notes(request.getNotes())
                .accountId(request.getAccountId())
                .destinationAccountId(request.getDestinationAccountId())
                .recurring(groupId != null)
                .recurrenceFrequency(groupId != null ? request.getRecurrenceFrequency() : null)
                .recurrenceGroupId(groupId)
                .recurrenceEndDate(endDate)
                .category(category)
                .user(user)
                .hidden(false)
                .build();
    }

    private List<LocalDate> computeOccurrenceDates(LocalDate start, String frequency, LocalDate end) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            dates.add(current);
            current = switch (frequency) {
                case "DAILY"     -> current.plusDays(1);
                case "WEEKLY"    -> current.plusWeeks(1);
                case "BIWEEKLY"  -> current.plusWeeks(2);
                case "MONTHLY"   -> current.plusMonths(1);
                case "QUARTERLY" -> current.plusMonths(3);
                case "YEARLY"    -> current.plusYears(1);
                default          -> end.plusDays(1); // stop
            };
        }
        return dates;
    }

    public TransactionResponse update(Long id, TransactionRequest request) {
        User user = currentUser();
        Transaction transaction = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
        Category category = resolveCategory(request, user);

        if (isBalanceActive(transaction)) {
            applyBalanceEffect(transaction.getType(), transaction.getAccountId(), transaction.getDestinationAccountId(), transaction.getAmount(), true);
        }
        if (!Boolean.TRUE.equals(request.getRecurring()) || !request.getTransactionDate().isAfter(LocalDate.now())) {
            if (!Boolean.TRUE.equals(transaction.getHidden())) {
                applyBalanceEffect(request.getType(), request.getAccountId(), request.getDestinationAccountId(), request.getAmount(), false);
            }
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

        rebalanceAccounts(transaction.getAccountId(), transaction.getDestinationAccountId());
        rebalanceAccounts(request.getAccountId(), request.getDestinationAccountId());

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
        applyBalanceEffect(transaction.getType(), transaction.getAccountId(), transaction.getDestinationAccountId(), transaction.getAmount(), nowHidden);
        rebalanceAccounts(transaction.getAccountId(), transaction.getDestinationAccountId());
        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    public void delete(Long id) {
        Transaction transaction = transactionRepository.findByIdAndUser(id, currentUser())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
        if (isBalanceActive(transaction)) {
            applyBalanceEffect(transaction.getType(), transaction.getAccountId(), transaction.getDestinationAccountId(), transaction.getAmount(), true);
        }
        rebalanceAccounts(transaction.getAccountId(), transaction.getDestinationAccountId());
        transactionRepository.delete(transaction);
    }

    public void deleteFutureFromGroup(Long id) {
        User user = currentUser();
        Transaction pivot = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));

        String groupId = pivot.getRecurrenceGroupId();
        if (groupId == null) { delete(id); return; }

        LocalDate fromDate = pivot.getTransactionDate();
        List<Transaction> toDelete = transactionRepository.findByRecurrenceGroupIdAndUser(groupId, user)
                .stream()
                .filter(t -> !t.getTransactionDate().isBefore(fromDate))
                .toList();

        for (Transaction tx : toDelete) {
            if (isBalanceActive(tx)) {
                applyBalanceEffect(tx.getType(), tx.getAccountId(), tx.getDestinationAccountId(), tx.getAmount(), true);
            }
        }
        transactionRepository.deleteAll(toDelete);
        rebalanceAccounts(pivot.getAccountId(), pivot.getDestinationAccountId());
    }

    private boolean isBalanceActive(Transaction tx) {
        return !Boolean.TRUE.equals(tx.getHidden())
                && tx.getTransactionDate() != null
                && !tx.getTransactionDate().isAfter(LocalDate.now());
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private void rebalanceAccounts(Long accountId, Long destinationAccountId) {
        goalService.rebalanceIfNeeded(accountId);
        goalService.rebalanceIfNeeded(destinationAccountId);
    }

    private Category resolveCategory(TransactionRequest request, User user) {
        if (request.getType() == TransactionType.TRANSFER || request.getCategoryId() == null) return null;
        return categoryRepository.findByIdAndUserOrDefault(request.getCategoryId(), user)
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
    }

    private void applyBalanceEffect(TransactionType type, Long accountId, Long destinationAccountId, BigDecimal amount, boolean reverse) {
        if (amount == null) return;
        if (type == TransactionType.TRANSFER) {
            adjustSingleBalance(accountId, amount, reverse);
            adjustSingleBalance(destinationAccountId, amount, !reverse);
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
