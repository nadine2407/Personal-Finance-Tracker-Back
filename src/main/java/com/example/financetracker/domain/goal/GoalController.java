package com.example.financetracker.domain.goal;

import com.example.financetracker.domain.goal.dto.DepositRequest;
import com.example.financetracker.domain.goal.dto.GoalRequest;
import com.example.financetracker.domain.goal.dto.GoalResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @GetMapping
    public ResponseEntity<List<GoalResponse>> getAll() {
        return ResponseEntity.ok(goalService.getAll());
    }

    @PostMapping
    public ResponseEntity<GoalResponse> create(@Valid @RequestBody GoalRequest request) {
        return ResponseEntity.ok(goalService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody GoalRequest request) {
        return ResponseEntity.ok(goalService.update(id, request));
    }

    @PatchMapping("/{id}/deposit")
    public ResponseEntity<GoalResponse> deposit(@PathVariable Long id,
                                                 @Valid @RequestBody DepositRequest request) {
        return ResponseEntity.ok(goalService.deposit(id, request));
    }

    @PatchMapping("/{id}/withdraw")
    public ResponseEntity<GoalResponse> withdraw(@PathVariable Long id,
                                                  @Valid @RequestBody DepositRequest request) {
        return ResponseEntity.ok(goalService.withdraw(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        goalService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
