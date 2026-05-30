package com.example.financetracker.domain.account;

import com.example.financetracker.domain.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUser(User user);
    Optional<Account> findByIdAndUser(Long id, User user);
}
