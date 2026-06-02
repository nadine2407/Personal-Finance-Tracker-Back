package com.example.financetracker.domain.goal;

import com.example.financetracker.domain.goal.dto.DepositRequest;
import com.example.financetracker.domain.goal.dto.GoalRequest;
import com.example.financetracker.domain.goal.dto.GoalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/goals")
@Tag(name = "Goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @GetMapping
    @Operation(summary = "Get all savings goals")
    public ResponseEntity<List<GoalResponse>> getAll() {
        return ResponseEntity.ok(goalService.getAll());
    }

    @PostMapping
    @Operation(summary = "Create a savings goal")
    public ResponseEntity<GoalResponse> create(@Valid @RequestBody GoalRequest request) {
        return ResponseEntity.ok(goalService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a savings goal")
    public ResponseEntity<GoalResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody GoalRequest request) {
        return ResponseEntity.ok(goalService.update(id, request));
    }

    @PatchMapping("/{id}/deposits")
    @Operation(summary = "Add funds to a savings goal")
    public ResponseEntity<GoalResponse> deposit(@PathVariable Long id,
                                                 @Valid @RequestBody DepositRequest request) {
        return ResponseEntity.ok(goalService.deposit(id, request));
    }

    @PatchMapping("/{id}/withdrawals")
    @Operation(summary = "Withdraw funds from a savings goal")
    public ResponseEntity<GoalResponse> withdraw(@PathVariable Long id,
                                                  @Valid @RequestBody DepositRequest request) {
        return ResponseEntity.ok(goalService.withdraw(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a savings goal")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        goalService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
