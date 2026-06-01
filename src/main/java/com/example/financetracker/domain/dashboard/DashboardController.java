package com.example.financetracker.domain.dashboard;

import com.example.financetracker.domain.dashboard.dto.DashboardSummary;
import com.example.financetracker.domain.dashboard.dto.MonthlyChart;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Get monthly summary (income, expenses, savings, category breakdown)")
    public ResponseEntity<DashboardSummary> getSummary(
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(dashboardService.getSummary(year, month));
    }

    @GetMapping("/monthly-chart")
    @Operation(summary = "Get monthly income/expenses chart data for a full year")
    public ResponseEntity<MonthlyChart> getMonthlyChart(@RequestParam int year) {
        return ResponseEntity.ok(dashboardService.getMonthlyChart(year));
    }
}
