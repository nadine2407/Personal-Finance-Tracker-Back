package com.example.financetracker.domain.budget;

import com.example.financetracker.domain.budget.dto.BudgetPageSummary;
import com.example.financetracker.domain.budget.dto.BudgetRequest;
import com.example.financetracker.domain.budget.dto.BudgetResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping("/status")
    public ResponseEntity<BudgetPageSummary> getStatus(
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(budgetService.getStatus(month, year));
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> create(@Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.ok(budgetService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.ok(budgetService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        budgetService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/duplicate")
    public ResponseEntity<List<BudgetResponse>> duplicate(
            @RequestParam int fromMonth,
            @RequestParam int fromYear,
            @RequestParam int toMonth,
            @RequestParam int toYear) {
        return ResponseEntity.ok(budgetService.duplicate(fromMonth, fromYear, toMonth, toYear));
    }
}
