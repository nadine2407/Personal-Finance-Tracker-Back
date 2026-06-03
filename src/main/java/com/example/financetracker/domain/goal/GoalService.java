package com.example.financetracker.domain.goal;

import com.example.financetracker.common.exception.ResourceNotFoundException;
import com.example.financetracker.domain.account.Account;
import com.example.financetracker.domain.account.AccountRepository;
import com.example.financetracker.domain.account.AccountType;
import com.example.financetracker.domain.auth.User;
import com.example.financetracker.domain.auth.UserRepository;
import com.example.financetracker.domain.goal.dto.AllocationRequest;
import com.example.financetracker.domain.goal.dto.DebitRequest;
import com.example.financetracker.domain.goal.dto.GoalRequest;
import com.example.financetracker.domain.goal.dto.GoalResponse;
import com.example.financetracker.domain.transaction.Transaction;
import com.example.financetracker.domain.transaction.TransactionRepository;
import com.example.financetracker.domain.transaction.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public List<GoalResponse> getAll() {
        LocalDate now = LocalDate.now();
        return goalRepository.findByUser(currentUser()).stream()
                .filter(g -> !g.isDebited() || (g.getDebitedAt() != null
                        && g.getDebitedAt().getMonth() == now.getMonth()
                        && g.getDebitedAt().getYear() == now.getYear()))
                .map(GoalResponse::from)
                .toList();
    }

    public GoalResponse debit(Long id, DebitRequest request) {
        User user = currentUser();
        Goal goal = goalRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", id));

        if (goal.isDebited()) throw new IllegalStateException("Cet objectif a déjà été débité");

        BigDecimal amount = goal.getLinkedAccountAmount() != null ? goal.getLinkedAccountAmount() : BigDecimal.ZERO;
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalStateException("Aucun montant alloué à débiter");

        Account savingsAccount = goal.getLinkedAccount();
        Account checkingAccount = accountRepository.findByIdAndUser(request.getCheckingAccountId(), user)
                .orElseThrow(() -> new ResourceNotFoundException("Account", request.getCheckingAccountId()));

        // Désallouer l'objectif avant de toucher au solde
        goal.setLinkedAccountAmount(BigDecimal.ZERO);
        goal.setCurrentAmount(BigDecimal.ZERO);

        // 1. TRANSFER épargne → courant (virement)
        transactionRepository.save(Transaction.builder()
                .type(TransactionType.TRANSFER)
                .amount(amount)
                .description("Virement objectif : " + goal.getName())
                .transactionDate(LocalDate.now())
                .accountId(savingsAccount.getId())
                .destinationAccountId(checkingAccount.getId())
                .user(user)
                .recurring(false)
                .hidden(false)
                .build());
        savingsAccount.setCurrentBalance(savingsAccount.getCurrentBalance().subtract(amount));
        accountRepository.save(savingsAccount);
        checkingAccount.setCurrentBalance(checkingAccount.getCurrentBalance().add(amount));
        accountRepository.save(checkingAccount);

        // 2. EXPENSE sur le compte courant (débit immédiat)
        transactionRepository.save(Transaction.builder()
                .type(TransactionType.EXPENSE)
                .amount(amount)
                .description("Objectif atteint : " + goal.getName())
                .transactionDate(LocalDate.now())
                .accountId(checkingAccount.getId())
                .user(user)
                .recurring(false)
                .hidden(false)
                .build());
        checkingAccount.setCurrentBalance(checkingAccount.getCurrentBalance().subtract(amount));
        accountRepository.save(checkingAccount);

        // Marquer l'objectif comme débité
        goal.setDebited(true);
        goal.setDebitedAt(LocalDate.now());
        return GoalResponse.from(goalRepository.save(goal));
    }

    public GoalResponse create(GoalRequest request) {
        User user = currentUser();

        Account linkedAccount = resolveLinkedAccount(request.getLinkedAccountId(), user);
        BigDecimal allocated = request.getAllocatedAmount() != null ? request.getAllocatedAmount() : BigDecimal.ZERO;

        if (allocated.compareTo(BigDecimal.ZERO) > 0) {
            validateAllocation(linkedAccount, null, allocated);
        }

        int priority = goalRepository.findMaxPriorityByLinkedAccountAndUser(linkedAccount, user) + 1;

        Goal goal = Goal.builder()
                .name(request.getName())
                .targetAmount(request.getTargetAmount())
                .currentAmount(allocated)
                .deadline(request.getDeadline())
                .description(request.getDescription())
                .linkedAccount(linkedAccount)
                .linkedAccountAmount(allocated)
                .priority(priority)
                .user(user)
                .build();

        return GoalResponse.from(goalRepository.save(goal));
    }

    public GoalResponse update(Long id, GoalRequest request) {
        User user = currentUser();
        Goal goal = goalRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", id));

        goal.setName(request.getName());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setDeadline(request.getDeadline());
        goal.setDescription(request.getDescription());

        return GoalResponse.from(goalRepository.save(goal));
    }

    /**
     * Modifie le montant alloué à un objectif (valeur absolue, pas un delta).
     * Valide que la somme totale des allocations ne dépasse pas le solde du compte.
     */
    public GoalResponse allocate(Long id, AllocationRequest request) {
        User user = currentUser();
        Goal goal = goalRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", id));

        if (goal.getLinkedAccount() != null) {
            validateAllocation(goal.getLinkedAccount(), id, request.getAllocatedAmount());
        }

        goal.setLinkedAccountAmount(request.getAllocatedAmount());
        goal.setCurrentAmount(request.getAllocatedAmount());

        return GoalResponse.from(goalRepository.save(goal));
    }

    /**
     * Remonte ou descend un objectif dans l'ordre de priorité de son compte.
     * Priorité 1 = la plus haute (sera préservée en dernier lors d'un retrait en cascade).
     */
    public void movePriority(Long id, String direction) {
        User user = currentUser();
        Goal goal = goalRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", id));

        if (goal.getLinkedAccount() == null) return;

        List<Goal> siblings = goalRepository.findByLinkedAccountAndUserOrderByPriorityAsc(goal.getLinkedAccount(), user);
        int idx = -1;
        for (int i = 0; i < siblings.size(); i++) {
            if (siblings.get(i).getId().equals(id)) { idx = i; break; }
        }
        if (idx == -1) return;

        int swapIdx = "up".equals(direction) ? idx - 1 : idx + 1;
        if (swapIdx < 0 || swapIdx >= siblings.size()) return;

        Goal other = siblings.get(swapIdx);
        Integer tmp = goal.getPriority();
        goal.setPriority(other.getPriority());
        other.setPriority(tmp);

        goalRepository.save(goal);
        goalRepository.save(other);
    }

    public void delete(Long id) {
        Goal goal = goalRepository.findByIdAndUser(id, currentUser())
                .orElseThrow(() -> new ResourceNotFoundException("Goal", id));
        goalRepository.delete(goal);
    }

    /**
     * Rééquilibre automatiquement les allocations d'un compte épargne après un débit.
     * Applique la règle en cascade : épargne libre d'abord, puis objectifs du moins prioritaire au plus prioritaire.
     * Appelé depuis TransactionService après chaque modification de solde.
     */
    public void rebalanceIfNeeded(Long accountId) {
        if (accountId == null) return;
        accountRepository.findById(accountId).ifPresent(account -> {
            if (account.getType() != AccountType.SAVINGS) return;

            BigDecimal balance = account.getCurrentBalance() != null
                    ? account.getCurrentBalance()
                    : BigDecimal.ZERO;
            BigDecimal totalAllocated = goalRepository.sumLinkedAccountAmountByAccount(account);

            if (totalAllocated.compareTo(balance) <= 0) return; // rien à faire

            // Excédent à absorber en partant des objectifs les moins prioritaires
            BigDecimal excess = totalAllocated.subtract(balance);
            List<Goal> goals = goalRepository.findByLinkedAccountOrderByPriorityDesc(account);

            for (Goal goal : goals) {
                if (excess.compareTo(BigDecimal.ZERO) <= 0) break;
                BigDecimal current = goal.getLinkedAccountAmount() != null ? goal.getLinkedAccountAmount() : BigDecimal.ZERO;
                BigDecimal reduce = current.min(excess);
                if (reduce.compareTo(BigDecimal.ZERO) > 0) {
                    goal.setLinkedAccountAmount(current.subtract(reduce));
                    goal.setCurrentAmount(goal.getLinkedAccountAmount());
                    goalRepository.save(goal);
                    excess = excess.subtract(reduce);
                }
            }
        });
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Account resolveLinkedAccount(Long accountId, User user) {
        if (accountId == null) throw new IllegalArgumentException("Un objectif doit être lié à un compte épargne");
        Account account = accountRepository.findByIdAndUser(accountId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));
        if (account.getType() != AccountType.SAVINGS) {
            throw new IllegalArgumentException("Les objectifs ne peuvent être liés qu'à un compte épargne");
        }
        return account;
    }

    private void validateAllocation(Account account, Long excludeGoalId, BigDecimal newAmount) {
        BigDecimal totalAllocated = goalRepository.sumLinkedAccountAmountByAccount(account);

        if (excludeGoalId != null) {
            goalRepository.findById(excludeGoalId).ifPresent(existing -> {
                // will be captured in lambda but we need effective-final trick
            });
            // Manual subtraction of current goal's allocation
            BigDecimal currentAlloc = goalRepository.findById(excludeGoalId)
                    .map(g -> g.getLinkedAccountAmount() != null ? g.getLinkedAccountAmount() : BigDecimal.ZERO)
                    .orElse(BigDecimal.ZERO);
            totalAllocated = totalAllocated.subtract(currentAlloc);
        }

        BigDecimal balance = account.getCurrentBalance() != null
                ? account.getCurrentBalance()
                : account.getInitialBalance();

        if (totalAllocated.add(newAmount).compareTo(balance) > 0) {
            throw new IllegalArgumentException("Le montant alloué dépasse le solde disponible du compte épargne");
        }
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", 0L));
    }
}
