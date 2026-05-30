package com.example.financetracker.domain.dashboard;

import com.example.financetracker.common.exception.ResourceNotFoundException;
import com.example.financetracker.domain.auth.User;
import com.example.financetracker.domain.auth.UserRepository;
import com.example.financetracker.domain.budget.BudgetRepository;
import com.example.financetracker.domain.category.CategoryType;
import com.example.financetracker.domain.dashboard.dto.DashboardResponse;
import com.example.financetracker.domain.goal.GoalRepository;
import com.example.financetracker.domain.transaction.Transaction;
import com.example.financetracker.domain.transaction.TransactionRepository;
import com.example.financetracker.domain.transaction.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final GoalRepository goalRepository;
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;

    public DashboardResponse getSummary() {
        User user = currentUser();

        List<Transaction> allTransactions = transactionRepository.findByUserOrderByDateDesc(user);

        BigDecimal totalIncome = transactionRepository
                .findByUserAndCategory_TypeOrderByDateDesc(user, CategoryType.INCOME)
                .stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = transactionRepository
                .findByUserAndCategory_TypeOrderByDateDesc(user, CategoryType.EXPENSE)
                .stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<TransactionResponse> recent = allTransactions.stream()
                .limit(5)
                .map(TransactionResponse::from)
                .toList();

        DashboardResponse response = new DashboardResponse();
        response.setTotalIncome(totalIncome);
        response.setTotalExpenses(totalExpenses);
        response.setBalance(totalIncome.subtract(totalExpenses));
        response.setTotalTransactions(allTransactions.size());
        response.setTotalGoals(goalRepository.findByUser(user).size());
        response.setTotalBudgets(budgetRepository.findByUser(user).size());
        response.setRecentTransactions(recent);
        return response;
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", 0L));
    }
}
