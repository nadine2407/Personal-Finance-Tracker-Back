package com.example.financetracker.domain.goal;

import com.example.financetracker.common.dto.ApiResponse;
import com.example.financetracker.domain.goal.dto.GoalRequest;
import com.example.financetracker.domain.goal.dto.GoalResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<GoalResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(goalService.getAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GoalResponse>> create(@Valid @RequestBody GoalRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(goalService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GoalResponse>> update(@PathVariable Long id,
                                                            @Valid @RequestBody GoalRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(goalService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        goalService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
