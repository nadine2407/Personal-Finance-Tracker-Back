package com.example.financetracker.domain.transaction;

import com.example.financetracker.domain.auth.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUser(Long id, User user);

    List<Transaction> findByUserAndTransactionDateBetween(User user, LocalDate from, LocalDate to);

    List<Transaction> findByUserAndTypeAndTransactionDateBetween(
            User user, TransactionType type, LocalDate from, LocalDate to);
}
