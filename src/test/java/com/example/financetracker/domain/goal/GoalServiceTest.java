package com.example.financetracker.domain.goal;

import com.example.financetracker.common.exception.ResourceNotFoundException;
import com.example.financetracker.domain.account.Account;
import com.example.financetracker.domain.account.AccountRepository;
import com.example.financetracker.domain.account.AccountType;
import com.example.financetracker.domain.auth.User;
import com.example.financetracker.domain.auth.UserRepository;
import com.example.financetracker.domain.goal.dto.AllocationRequest;
import com.example.financetracker.domain.goal.dto.GoalRequest;
import com.example.financetracker.domain.goal.dto.GoalResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock private GoalRepository goalRepository;
    @Mock private UserRepository userRepository;
    @Mock private AccountRepository accountRepository;

    @InjectMocks
    private GoalService goalService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("test@test.com")
                .firstName("Test").lastName("User").passwordHash("hash").build();

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("test@test.com");
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
    }

    @Test
    void create_shouldInitializeAllocatedAmountToZero() {
        Account account = Account.builder().id(10L).name("Livret A")
                .type(AccountType.SAVINGS)
                .currentBalance(new BigDecimal("5000.00"))
                .initialBalance(new BigDecimal("5000.00"))
                .user(user).build();

        GoalRequest request = new GoalRequest();
        request.setName("Vacances");
        request.setTargetAmount(new BigDecimal("2000.00"));
        request.setLinkedAccountId(10L);

        when(accountRepository.findByIdAndUser(10L, user)).thenReturn(Optional.of(account));
        when(goalRepository.findMaxPriorityByLinkedAccountAndUser(account, user)).thenReturn(0);

        Goal savedGoal = Goal.builder().id(1L).name("Vacances")
                .targetAmount(new BigDecimal("2000.00"))
                .currentAmount(BigDecimal.ZERO)
                .linkedAccountAmount(BigDecimal.ZERO)
                .linkedAccount(account)
                .user(user).build();
        when(goalRepository.save(any())).thenReturn(savedGoal);

        GoalResponse response = goalService.create(request);

        assertThat(response.getAllocatedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void allocate_shouldSetAllocatedAmount() {
        Account account = Account.builder().id(10L).name("Livret A")
                .type(AccountType.SAVINGS)
                .currentBalance(new BigDecimal("5000.00"))
                .initialBalance(new BigDecimal("5000.00"))
                .user(user).build();

        Goal goal = Goal.builder().id(1L).name("Vacances")
                .targetAmount(new BigDecimal("2000.00"))
                .currentAmount(BigDecimal.ZERO)
                .linkedAccountAmount(BigDecimal.ZERO)
                .linkedAccount(account)
                .user(user).build();

        AllocationRequest request = new AllocationRequest();
        request.setAllocatedAmount(new BigDecimal("800.00"));

        when(goalRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(goal));
        when(goalRepository.sumLinkedAccountAmountByAccount(account)).thenReturn(BigDecimal.ZERO);
        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(goalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GoalResponse response = goalService.allocate(1L, request);

        assertThat(response.getAllocatedAmount()).isEqualByComparingTo("800.00");
    }

    @Test
    void allocate_goalNotFound_shouldThrow() {
        when(goalRepository.findByIdAndUser(99L, user)).thenReturn(Optional.empty());

        AllocationRequest request = new AllocationRequest();
        request.setAllocatedAmount(new BigDecimal("100.00"));

        assertThatThrownBy(() -> goalService.allocate(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void allocate_whenReachesTarget_shouldBeCompleted() {
        Account account = Account.builder().id(10L).name("Livret A")
                .type(AccountType.SAVINGS)
                .currentBalance(new BigDecimal("5000.00"))
                .initialBalance(new BigDecimal("5000.00"))
                .user(user).build();

        Goal goal = Goal.builder().id(1L).name("Vacances")
                .targetAmount(new BigDecimal("1000.00"))
                .currentAmount(new BigDecimal("900.00"))
                .linkedAccountAmount(new BigDecimal("900.00"))
                .linkedAccount(account)
                .user(user).build();

        AllocationRequest request = new AllocationRequest();
        request.setAllocatedAmount(new BigDecimal("1000.00"));

        when(goalRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(goal));
        when(goalRepository.sumLinkedAccountAmountByAccount(account)).thenReturn(new BigDecimal("900.00"));
        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(goalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GoalResponse response = goalService.allocate(1L, request);

        assertThat(response.isCompleted()).isTrue();
        assertThat(response.getProgressPercent()).isEqualTo(100.0);
    }
}
