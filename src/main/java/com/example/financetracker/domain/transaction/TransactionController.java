package com.example.financetracker.domain.transaction;

import com.example.financetracker.common.dto.ApiResponse;
import com.example.financetracker.domain.category.CategoryType;
import com.example.financetracker.domain.transaction.dto.TransactionRequest;
import com.example.financetracker.domain.transaction.dto.TransactionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(transactionService.getAll()));
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> filter(
            @RequestParam(required = false) CategoryType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(transactionService.filter(type, from, to)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> create(@Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(transactionService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> update(@PathVariable Long id,
                                                                    @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(transactionService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        transactionService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
