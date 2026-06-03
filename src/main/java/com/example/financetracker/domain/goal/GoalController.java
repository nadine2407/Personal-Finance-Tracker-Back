package com.example.financetracker.domain.goal;

import com.example.financetracker.domain.goal.dto.AllocationRequest;
import com.example.financetracker.domain.goal.dto.DebitRequest;
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

    @PatchMapping("/{id}/allocation")
    @Operation(summary = "Set the allocated amount for a goal (absolute value)")
    public ResponseEntity<GoalResponse> allocate(@PathVariable Long id,
                                                  @Valid @RequestBody AllocationRequest request) {
        return ResponseEntity.ok(goalService.allocate(id, request));
    }

    @PatchMapping("/{id}/priority")
    @Operation(summary = "Move a goal up or down in priority order")
    public ResponseEntity<Void> movePriority(@PathVariable Long id,
                                              @RequestParam String direction) {
        goalService.movePriority(id, direction);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/debit")
    @Operation(summary = "Debit a completed goal: creates EXPENSE on savings and INCOME on checking account")
    public ResponseEntity<GoalResponse> debit(@PathVariable Long id,
                                               @Valid @RequestBody DebitRequest request) {
        return ResponseEntity.ok(goalService.debit(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a savings goal")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        goalService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
