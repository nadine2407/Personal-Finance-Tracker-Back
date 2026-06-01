package com.example.financetracker.domain.goal;

import com.example.financetracker.common.exception.ResourceNotFoundException;
import com.example.financetracker.domain.account.Account;
import com.example.financetracker.domain.account.AccountRepository;
import com.example.financetracker.domain.auth.User;
import com.example.financetracker.domain.auth.UserRepository;
import com.example.financetracker.domain.goal.dto.DepositRequest;
import com.example.financetracker.domain.goal.dto.GoalRequest;
import com.example.financetracker.domain.goal.dto.GoalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public List<GoalResponse> getAll() {
        return goalRepository.findByUser(currentUser()).stream()
                .map(GoalResponse::from)
                .toList();
    }

    public GoalResponse create(GoalRequest request) {
        Goal goal = Goal.builder()
                .name(request.getName())
                .targetAmount(request.getTargetAmount())
                .currentAmount(BigDecimal.ZERO)
                .deadline(request.getDeadline())
                .description(request.getDescription())
                .user(currentUser())
                .build();
        return GoalResponse.from(goalRepository.save(goal));
    }

    public GoalResponse update(Long id, GoalRequest request) {
        Goal goal = goalRepository.findByIdAndUser(id, currentUser())
                .orElseThrow(() -> new ResourceNotFoundException("Goal", id));
        goal.setName(request.getName());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setDeadline(request.getDeadline());
        goal.setDescription(request.getDescription());
        return GoalResponse.from(goalRepository.save(goal));
    }

    public GoalResponse deposit(Long id, DepositRequest request) {
        Goal goal = goalRepository.findByIdAndUser(id, currentUser())
                .orElseThrow(() -> new ResourceNotFoundException("Goal", id));
        BigDecimal current = goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO;
        goal.setCurrentAmount(current.add(request.getAmount()));

        if (request.getAccountId() != null) {
            Account account = accountRepository.findByIdAndUser(request.getAccountId(), currentUser())
                    .orElseThrow(() -> new ResourceNotFoundException("Account", request.getAccountId()));
            goal.setLinkedAccount(account);
            BigDecimal linked = goal.getLinkedAccountAmount() != null ? goal.getLinkedAccountAmount() : BigDecimal.ZERO;
            goal.setLinkedAccountAmount(linked.add(request.getAmount()));
        }

        return GoalResponse.from(goalRepository.save(goal));
    }

    public GoalResponse withdraw(Long id, DepositRequest request) {
        Goal goal = goalRepository.findByIdAndUser(id, currentUser())
                .orElseThrow(() -> new ResourceNotFoundException("Goal", id));
        BigDecimal current = goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO;
        goal.setCurrentAmount(current.subtract(request.getAmount()).max(BigDecimal.ZERO));

        if (goal.getLinkedAccountAmount() != null && goal.getLinkedAccountAmount().compareTo(BigDecimal.ZERO) > 0) {
            goal.setLinkedAccountAmount(goal.getLinkedAccountAmount().subtract(request.getAmount()).max(BigDecimal.ZERO));
        }

        return GoalResponse.from(goalRepository.save(goal));
    }

    public void delete(Long id) {
        Goal goal = goalRepository.findByIdAndUser(id, currentUser())
                .orElseThrow(() -> new ResourceNotFoundException("Goal", id));
        goalRepository.delete(goal);
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", 0L));
    }
}
