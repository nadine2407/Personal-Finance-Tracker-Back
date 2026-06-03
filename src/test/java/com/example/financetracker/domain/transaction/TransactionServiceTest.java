package com.example.financetracker.domain.transaction;

import com.example.financetracker.common.exception.ResourceNotFoundException;
import com.example.financetracker.domain.account.Account;
import com.example.financetracker.domain.account.AccountRepository;
import com.example.financetracker.domain.auth.User;
import com.example.financetracker.domain.auth.UserRepository;
import com.example.financetracker.domain.category.Category;
import com.example.financetracker.domain.category.CategoryRepository;
import com.example.financetracker.domain.transaction.dto.TransactionRequest;
import com.example.financetracker.domain.transaction.dto.TransactionResponse;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User user;
    private Account account;
    private Category category;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("test@test.com")
                .firstName("Test").lastName("User").passwordHash("hash").build();
        account = Account.builder().id(10L).name("Compte courant")
                .currentBalance(new BigDecimal("1000.00")).build();
        category = Category.builder().id(5L).name("Salaire").build();

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("test@test.com");
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
    }

    @Test
    void create_income_shouldIncreaseAccountBalance() {
        TransactionRequest request = new TransactionRequest();
        request.setType(TransactionType.INCOME);
        request.setAmount(new BigDecimal("500.00"));
        request.setTransactionDate(LocalDate.now());
        request.setCategoryId(5L);
        request.setAccountId(10L);

        when(categoryRepository.findByIdAndUserOrDefault(5L, user)).thenReturn(Optional.of(category));
        Transaction saved = Transaction.builder().id(1L).type(TransactionType.INCOME)
                .amount(new BigDecimal("500.00")).transactionDate(LocalDate.now())
                .category(category).user(user).recurring(false).build();
        when(transactionRepository.save(any())).thenReturn(saved);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenReturn(account);

        transactionService.create(request);

        // solde 1000 + 500 = 1500
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("1500.00");
    }

    @Test
    void create_expense_shouldDecreaseAccountBalance() {
        TransactionRequest request = new TransactionRequest();
        request.setType(TransactionType.EXPENSE);
        request.setAmount(new BigDecimal("200.00"));
        request.setTransactionDate(LocalDate.now());
        request.setCategoryId(5L);
        request.setAccountId(10L);

        when(categoryRepository.findByIdAndUserOrDefault(5L, user)).thenReturn(Optional.of(category));
        Transaction saved = Transaction.builder().id(1L).type(TransactionType.EXPENSE)
                .amount(new BigDecimal("200.00")).transactionDate(LocalDate.now())
                .category(category).user(user).recurring(false).build();
        when(transactionRepository.save(any())).thenReturn(saved);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenReturn(account);

        transactionService.create(request);

        // solde 1000 - 200 = 800
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("800.00");
    }

    @Test
    void create_withNoAccount_shouldNotFail() {
        TransactionRequest request = new TransactionRequest();
        request.setType(TransactionType.EXPENSE);
        request.setAmount(new BigDecimal("100.00"));
        request.setTransactionDate(LocalDate.now());
        request.setCategoryId(5L);
        request.setAccountId(null);

        when(categoryRepository.findByIdAndUserOrDefault(5L, user)).thenReturn(Optional.of(category));
        Transaction saved = Transaction.builder().id(1L).type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.00")).transactionDate(LocalDate.now())
                .category(category).user(user).recurring(false).build();
        when(transactionRepository.save(any())).thenReturn(saved);

        assertThatNoException().isThrownBy(() -> transactionService.create(request));
        verify(accountRepository, never()).findById(any());
    }

    @Test
    void update_shouldReverseOldBalanceAndApplyNew() {
        Transaction existing = Transaction.builder().id(1L).type(TransactionType.EXPENSE)
                .amount(new BigDecimal("200.00")).accountId(10L)
                .category(category).user(user).recurring(false).build();

        TransactionRequest request = new TransactionRequest();
        request.setType(TransactionType.INCOME);
        request.setAmount(new BigDecimal("300.00"));
        request.setTransactionDate(LocalDate.now());
        request.setCategoryId(5L);
        request.setAccountId(10L);

        when(transactionRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(existing));
        when(categoryRepository.findByIdAndUserOrDefault(5L, user)).thenReturn(Optional.of(category));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenReturn(account);
        when(transactionRepository.save(any())).thenReturn(existing);

        transactionService.update(1L, request);

        // Ancienne dépense de 200 annulée (+200), puis nouveau revenu de 300 (+300)
        // 1000 + 200 + 300 = 1500
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("1500.00");
    }

    @Test
    void delete_shouldReverseTransactionBalance() {
        Transaction existing = Transaction.builder().id(1L).type(TransactionType.INCOME)
                .amount(new BigDecimal("500.00")).accountId(10L)
                .category(category).user(user).recurring(false).build();

        when(transactionRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(existing));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenReturn(account);

        transactionService.delete(1L);

        // Revenu de 500 annulé : 1000 - 500 = 500
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("500.00");
        verify(transactionRepository).delete(existing);
    }

    @Test
    void delete_transactionNotFound_shouldThrow() {
        when(transactionRepository.findByIdAndUser(99L, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
