package com.example.financetracker.domain.account;

import com.example.financetracker.common.exception.ResourceNotFoundException;
import com.example.financetracker.domain.auth.User;
import com.example.financetracker.domain.auth.UserRepository;
import com.example.financetracker.domain.account.dto.AccountRequest;
import com.example.financetracker.domain.account.dto.AccountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public List<AccountResponse> getAll() {
        return accountRepository.findByUser(currentUser()).stream()
                .map(AccountResponse::from)
                .toList();
    }

    public AccountResponse create(AccountRequest request) {
        Account account = Account.builder()
                .name(request.getName())
                .type(request.getType())
                .initialBalance(request.getInitialBalance())
                .currentBalance(request.getInitialBalance())
                .color(request.getColor())
                .icon(request.getIcon())
                .user(currentUser())
                .build();
        return AccountResponse.from(accountRepository.save(account));
    }

    public AccountResponse update(Long id, AccountRequest request) {
        Account account = accountRepository.findByIdAndUser(id, currentUser())
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
        account.setName(request.getName());
        account.setType(request.getType());
        account.setInitialBalance(request.getInitialBalance());
        account.setColor(request.getColor());
        account.setIcon(request.getIcon());
        return AccountResponse.from(accountRepository.save(account));
    }

    public void delete(Long id) {
        Account account = accountRepository.findByIdAndUser(id, currentUser())
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
        accountRepository.delete(account);
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", 0L));
    }
}
