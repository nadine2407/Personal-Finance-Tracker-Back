package com.example.financetracker.domain.budget;

import com.example.financetracker.common.exception.ResourceNotFoundException;
import com.example.financetracker.domain.auth.User;
import com.example.financetracker.domain.auth.UserRepository;
import com.example.financetracker.domain.budget.dto.BudgetPageSummary;
import com.example.financetracker.domain.budget.dto.BudgetRequest;
import com.example.financetracker.domain.budget.dto.BudgetResponse;
import com.example.financetracker.domain.budget.dto.BudgetStatusItem;
import com.example.financetracker.domain.category.Category;
import com.example.financetracker.domain.category.CategoryRepository;
import com.example.financetracker.domain.transaction.Transaction;
import com.example.financetracker.domain.transaction.TransactionRepository;
import com.example.financetracker.domain.transaction.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public BudgetPageSummary getStatus(int month, int year) {
        User user = currentUser();
        List<Budget> budgets = budgetRepository.findByUserAndMonthAndYear(user, month, year);

        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

        List<Transaction> expenses = transactionRepository.findByUserAndTypeAndTransactionDateBetween(
                user, TransactionType.EXPENSE, from, to);

        Map<Long, BigDecimal> spentByCategory = expenses.stream()
                .filter(t -> t.getCategory() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().getId(),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));

        List<BudgetStatusItem> items = budgets.stream().map(b -> {
            Long catId = b.getCategory() != null ? b.getCategory().getId() : null;
            BigDecimal spent = catId != null ? spentByCategory.getOrDefault(catId, BigDecimal.ZERO) : BigDecimal.ZERO;
            BigDecimal remaining = b.getAmount().subtract(spent);
            double pct = b.getAmount().compareTo(BigDecimal.ZERO) > 0
                    ? spent.multiply(BigDecimal.valueOf(100))
                            .divide(b.getAmount(), 2, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            BudgetStatus status;
            if (pct >= 100) status = BudgetStatus.OVER;
            else if (pct >= 80) status = BudgetStatus.WARN;
            else status = BudgetStatus.ON;

            return BudgetStatusItem.builder()
                    .id(b.getId())
                    .categoryId(catId)
                    .categoryName(b.getCategory() != null ? b.getCategory().getName() : null)
                    .categoryColor(b.getCategory() != null ? b.getCategory().getColor() : null)
                    .categoryIcon(b.getCategory() != null ? b.getCategory().getIcon() : null)
                    .budgetAmount(b.getAmount())
                    .spentAmount(spent)
                    .remainingAmount(remaining)
                    .percentage(pct)
                    .status(status)
                    .build();
        }).toList();

        BigDecimal totalBudget = items.stream().map(BudgetStatusItem::getBudgetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSpent = items.stream().map(BudgetStatusItem::getSpentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal overspend = items.stream()
                .filter(i -> i.getStatus() == BudgetStatus.OVER)
                .map(i -> i.getSpentAmount().subtract(i.getBudgetAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int warnCount = (int) items.stream().filter(i -> i.getStatus() == BudgetStatus.WARN).count();
        int overCount = (int) items.stream().filter(i -> i.getStatus() == BudgetStatus.OVER).count();

        return BudgetPageSummary.builder()
                .year(year)
                .month(month)
                .totalBudget(totalBudget)
                .totalSpent(totalSpent)
                .remaining(totalBudget.subtract(totalSpent))
                .overspend(overspend)
                .warnCount(warnCount)
                .overCount(overCount)
                .items(items)
                .build();
    }

    public BudgetResponse create(BudgetRequest request) {
        User user = currentUser();
        Category category = resolveCategory(request.getCategoryId(), user);
        Budget budget = Budget.builder()
                .amount(request.getAmount())
                .month(request.getMonth())
                .year(request.getYear())
                .category(category)
                .user(user)
                .build();
        return BudgetResponse.from(budgetRepository.save(budget));
    }

    public BudgetResponse update(Long id, BudgetRequest request) {
        User user = currentUser();
        Budget budget = budgetRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", id));
        Category updatedCategory = resolveCategory(request.getCategoryId(), user);
        budget.setAmount(request.getAmount());
        budget.setMonth(request.getMonth());
        budget.setYear(request.getYear());
        budget.setCategory(updatedCategory);
        return BudgetResponse.from(budgetRepository.save(budget));
    }

    public void delete(Long id) {
        Budget budget = budgetRepository.findByIdAndUser(id, currentUser())
                .orElseThrow(() -> new ResourceNotFoundException("Budget", id));
        budgetRepository.delete(budget);
    }

    public List<BudgetResponse> duplicate(int fromMonth, int fromYear, int toMonth, int toYear) {
        User user = currentUser();
        List<Budget> source = budgetRepository.findByUserAndMonthAndYear(user, fromMonth, fromYear);
        List<Budget> existing = budgetRepository.findByUserAndMonthAndYear(user, toMonth, toYear);

        Set<Long> existingCategoryIds = existing.stream()
                .filter(b -> b.getCategory() != null)
                .map(b -> b.getCategory().getId())
                .collect(Collectors.toSet());

        List<Budget> toAdd = source.stream()
                .filter(b -> b.getCategory() == null || !existingCategoryIds.contains(b.getCategory().getId()))
                .map(b -> Budget.builder()
                        .amount(b.getAmount())
                        .month(toMonth)
                        .year(toYear)
                        .category(b.getCategory())
                        .user(user)
                        .build())
                .toList();

        return budgetRepository.saveAll(toAdd).stream().map(BudgetResponse::from).toList();
    }

    private Category resolveCategory(Long categoryId, User user) {
        if (categoryId == null) return null;
        return categoryRepository.findByIdAndUserOrDefault(categoryId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", 0L));
    }
}
