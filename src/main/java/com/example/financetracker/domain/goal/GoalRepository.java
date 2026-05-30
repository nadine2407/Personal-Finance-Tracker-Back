package com.example.financetracker.domain.goal;

import com.example.financetracker.domain.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByUser(User user);
    Optional<Goal> findByIdAndUser(Long id, User user);
}
