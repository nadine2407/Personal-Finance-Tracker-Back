package com.example.financetracker.domain.goal;

import com.example.financetracker.common.exception.ResourceNotFoundException;
import com.example.financetracker.domain.auth.User;
import com.example.financetracker.domain.auth.UserRepository;
import com.example.financetracker.domain.goal.dto.DepositRequest;
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
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock private GoalRepository goalRepository;
    @Mock private UserRepository userRepository;

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
    void create_shouldInitializeCurrentAmountToZero() {
        GoalRequest request = new GoalRequest();
        request.setName("Vacances");
        request.setTargetAmount(new BigDecimal("2000.00"));
        request.setDeadline(LocalDate.now().plusMonths(6));

        Goal savedGoal = Goal.builder().id(1L).name("Vacances")
                .targetAmount(new BigDecimal("2000.00"))
                .currentAmount(BigDecimal.ZERO)
                .user(user).build();
        when(goalRepository.save(any())).thenReturn(savedGoal);

        GoalResponse response = goalService.create(request);

        assertThat(response.getCurrentAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void deposit_shouldAddAmountToCurrentAmount() {
        Goal goal = Goal.builder().id(1L).name("Vacances")
                .targetAmount(new BigDecimal("2000.00"))
                .currentAmount(new BigDecimal("500.00"))
                .user(user).build();

        DepositRequest request = new DepositRequest();
        request.setAmount(new BigDecimal("300.00"));

        when(goalRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(goal));
        when(goalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GoalResponse response = goalService.deposit(1L, request);

        // 500 + 300 = 800
        assertThat(response.getCurrentAmount()).isEqualByComparingTo("800.00");
    }

    @Test
    void withdraw_shouldSubtractAmountFromCurrentAmount() {
        Goal goal = Goal.builder().id(1L).name("Vacances")
                .targetAmount(new BigDecimal("2000.00"))
                .currentAmount(new BigDecimal("500.00"))
                .user(user).build();

        DepositRequest request = new DepositRequest();
        request.setAmount(new BigDecimal("200.00"));

        when(goalRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(goal));
        when(goalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GoalResponse response = goalService.withdraw(1L, request);

        // 500 - 200 = 300
        assertThat(response.getCurrentAmount()).isEqualByComparingTo("300.00");
    }

    @Test
    void withdraw_shouldNotGoBelowZero() {
        Goal goal = Goal.builder().id(1L).name("Vacances")
                .targetAmount(new BigDecimal("2000.00"))
                .currentAmount(new BigDecimal("100.00"))
                .user(user).build();

        DepositRequest request = new DepositRequest();
        request.setAmount(new BigDecimal("500.00")); // plus que disponible

        when(goalRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(goal));
        when(goalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GoalResponse response = goalService.withdraw(1L, request);

        assertThat(response.getCurrentAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void deposit_goalNotFound_shouldThrow() {
        when(goalRepository.findByIdAndUser(99L, user)).thenReturn(Optional.empty());

        DepositRequest request = new DepositRequest();
        request.setAmount(new BigDecimal("100.00"));

        assertThatThrownBy(() -> goalService.deposit(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deposit_whenGoalReachesTarget_shouldBeCompleted() {
        Goal goal = Goal.builder().id(1L).name("Vacances")
                .targetAmount(new BigDecimal("1000.00"))
                .currentAmount(new BigDecimal("900.00"))
                .user(user).build();

        DepositRequest request = new DepositRequest();
        request.setAmount(new BigDecimal("100.00"));

        when(goalRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(goal));
        when(goalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GoalResponse response = goalService.deposit(1L, request);

        assertThat(response.isCompleted()).isTrue();
        assertThat(response.getProgressPercent()).isEqualTo(100.0);
    }
}
