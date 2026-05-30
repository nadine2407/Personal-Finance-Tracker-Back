package com.example.financetracker.domain.transaction;

import com.example.financetracker.domain.auth.User;
import com.example.financetracker.domain.category.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserOrderByDateDesc(User user);
    Optional<Transaction> findByIdAndUser(Long id, User user);
    List<Transaction> findByUserAndCategory_TypeOrderByDateDesc(User user, CategoryType type);
    List<Transaction> findByUserAndDateBetweenOrderByDateDesc(User user, LocalDate from, LocalDate to);
}
