package com.example.financetracker.domain.goal;

import com.example.financetracker.domain.account.Account;
import com.example.financetracker.domain.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByUser(User user);
    Optional<Goal> findByIdAndUser(Long id, User user);

    @Query("SELECT COALESCE(SUM(g.linkedAccountAmount), 0) FROM Goal g WHERE g.linkedAccount = :account")
    BigDecimal sumLinkedAccountAmountByAccount(@Param("account") Account account);
}
