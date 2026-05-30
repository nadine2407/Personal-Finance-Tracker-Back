package com.example.financetracker.domain.goal;

import com.example.financetracker.common.exception.ResourceNotFoundException;
import com.example.financetracker.domain.auth.User;
import com.example.financetracker.domain.auth.UserRepository;
import com.example.financetracker.domain.goal.dto.GoalRequest;
import com.example.financetracker.domain.goal.dto.GoalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;

    public List<GoalResponse> getAll() {
        return goalRepository.findByUser(currentUser()).stream()
                .map(GoalResponse::from)
                .toList();
    }

    public GoalResponse create(GoalRequest request) {
        Goal goal = Goal.builder()
                .name(request.getName())
                .targetAmount(request.getTargetAmount())
                .savedAmount(request.getSavedAmount() != null ? request.getSavedAmount() : BigDecimal.ZERO)
                .targetDate(request.getTargetDate())
                .user(currentUser())
                .build();
        return GoalResponse.from(goalRepository.save(goal));
    }

    public GoalResponse update(Long id, GoalRequest request) {
        Goal goal = goalRepository.findByIdAndUser(id, currentUser())
                .orElseThrow(() -> new ResourceNotFoundException("Goal", id));
        goal.setName(request.getName());
        goal.setTargetAmount(request.getTargetAmount());
        if (request.getSavedAmount() != null) {
            goal.setSavedAmount(request.getSavedAmount());
        }
        goal.setTargetDate(request.getTargetDate());
        return GoalResponse.from(goalRepository.save(goal));
    }

    public void delete(Long id) {
        Goal goal = goalRepository.findByIdAndUser(id, currentUser())
                .orElseThrow(() -> new ResourceNotFoundException("Goal", id));
        goalRepository.delete(goal);
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", 0L));
    }
}
