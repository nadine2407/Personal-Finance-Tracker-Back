package com.example.financetracker.domain.dashboard;

import com.example.financetracker.common.exception.ResourceNotFoundException;
import com.example.financetracker.domain.auth.User;
import com.example.financetracker.domain.auth.UserRepository;
import com.example.financetracker.domain.category.Category;
import com.example.financetracker.domain.dashboard.dto.CategoryBreakdown;
import com.example.financetracker.domain.dashboard.dto.DashboardSummary;
import com.example.financetracker.domain.dashboard.dto.MonthlyChart;
import com.example.financetracker.domain.dashboard.dto.MonthlyDataPoint;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Transactional
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public DashboardSummary getSummary(int year, int month) {
        User user = currentUser();
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

        List<Transaction> transactions = transactionRepository.findByUserAndTransactionDateBetween(user, from, to)
                .stream().filter(t -> !Boolean.TRUE.equals(t.getHidden())).toList();

        BigDecimal totalIncome = sum(transactions, TransactionType.INCOME);
        BigDecimal totalExpenses = sum(transactions, TransactionType.EXPENSE);
        BigDecimal netSavings = totalIncome.subtract(totalExpenses);

        // Group expense amounts by category ID
        Map<Long, BigDecimal> amountByCatId = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE && t.getCategory() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().getId(),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));

        // Get a representative category object per ID
        Map<Long, Category> categoryById = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE && t.getCategory() != null)
                .collect(Collectors.toMap(
                        t -> t.getCategory().getId(),
                        Transaction::getCategory,
                        (a, b) -> a));

        List<CategoryBreakdown> breakdown = amountByCatId.entrySet().stream()
                .map(e -> {
                    Category cat = categoryById.get(e.getKey());
                    double pct = totalExpenses.compareTo(BigDecimal.ZERO) > 0
                            ? e.getValue().multiply(BigDecimal.valueOf(100))
                                    .divide(totalExpenses, 2, RoundingMode.HALF_UP).doubleValue()
                            : 0.0;
                    return CategoryBreakdown.builder()
                            .name(cat != null ? cat.getName() : "")
                            .color(cat != null ? cat.getColor() : null)
                            .icon(cat != null ? cat.getIcon() : null)
                            .amount(e.getValue().doubleValue())
                            .percentage(pct)
                            .build();
                })
                .sorted(Comparator.comparingDouble(CategoryBreakdown::getAmount).reversed())
                .toList();

        return DashboardSummary.builder()
                .year(year)
                .month(month)
                .totalIncome(totalIncome.doubleValue())
                .totalExpenses(totalExpenses.doubleValue())
                .netSavings(netSavings.doubleValue())
                .categoryBreakdown(breakdown)
                .build();
    }

    public MonthlyChart getMonthlyChart(int year) {
        User user = currentUser();
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);

        List<Transaction> transactions = transactionRepository.findByUserAndTransactionDateBetween(user, from, to)
                .stream().filter(t -> !Boolean.TRUE.equals(t.getHidden())).toList();

        Map<Integer, List<Transaction>> byMonth = transactions.stream()
                .filter(t -> t.getTransactionDate() != null)
                .collect(Collectors.groupingBy(t -> t.getTransactionDate().getMonthValue()));

        List<MonthlyDataPoint> data = IntStream.rangeClosed(1, 12)
                .mapToObj(m -> {
                    List<Transaction> monthTxns = byMonth.getOrDefault(m, List.of());
                    double income = monthTxns.stream()
                            .filter(t -> t.getType() == TransactionType.INCOME)
                            .mapToDouble(t -> t.getAmount().doubleValue())
                            .sum();
                    double expenses = monthTxns.stream()
                            .filter(t -> t.getType() == TransactionType.EXPENSE)
                            .mapToDouble(t -> t.getAmount().doubleValue())
                            .sum();
                    return MonthlyDataPoint.builder().month(m).income(income).expenses(expenses).build();
                })
                .toList();

        return MonthlyChart.builder().year(year).data(data).build();
    }

    private BigDecimal sum(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", 0L));
    }
}
