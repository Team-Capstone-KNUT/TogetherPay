package com.devcrew.togetherpay.domain.budget.controller;

import com.devcrew.togetherpay.domain.budget.dto.BudgetResponse;
import com.devcrew.togetherpay.domain.budget.dto.CreateBudgetRequest;
import com.devcrew.togetherpay.domain.budget.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/teams/{teamId}/budgets")
@RequiredArgsConstructor
public class BudgetController {
    private final BudgetService budgetService;

    @PostMapping
    public ResponseEntity<BudgetResponse> createDailyBudget(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long teamId,
            @Valid@RequestBody CreateBudgetRequest request) {

        BudgetResponse response = budgetService.createDailyBudget(
                userId, teamId, request.budgetDate(), request.amount()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

