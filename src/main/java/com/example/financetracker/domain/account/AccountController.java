package com.example.financetracker.domain.account;

import com.example.financetracker.common.dto.ApiResponse;
import com.example.financetracker.domain.account.dto.AccountRequest;
import com.example.financetracker.domain.account.dto.AccountResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(accountService.getAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> create(@Valid @RequestBody AccountRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(accountService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountResponse>> update(@PathVariable Long id,
                                                                @Valid @RequestBody AccountRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(accountService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        accountService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
