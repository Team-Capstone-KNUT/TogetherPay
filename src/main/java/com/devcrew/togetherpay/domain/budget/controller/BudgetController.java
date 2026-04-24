package com.devcrew.togetherpay.domain.budget.controller;

import com.devcrew.togetherpay.domain.budget.dto.BudgetResponse;
import com.devcrew.togetherpay.domain.budget.dto.CreateBudgetRequest;
import com.devcrew.togetherpay.domain.budget.dto.UpdateBudgetRequest;
import com.devcrew.togetherpay.domain.budget.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @PatchMapping("/{budgetId}")
    public ResponseEntity<BudgetResponse> updateBudget(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long budgetId,
            @RequestBody UpdateBudgetRequest request) {

        BudgetResponse response = budgetService.updateBudget(userId, budgetId, request.amount());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/trips/{tripId}")
    public ResponseEntity<BudgetResponse> getBudgetByTrip(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long tripId) {

        BudgetResponse response = budgetService.getBudgetByTrip(userId, tripId);

        return ResponseEntity.ok(response);

    }
}

