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
@RequestMapping("api/v1/teams/{teamId}/budgets")
@RequiredArgsConstructor
public class BudgetController {
    private final BudgetService budgetService;

    /**
     * 특정 날짜 예산 등록 API
     * @param userId
     * @param teamId
     * @param request
     * @return
     */
    @PostMapping
    public ResponseEntity<BudgetResponse> createDailyBudget(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long teamId,
            @Valid@RequestBody CreateBudgetRequest request) {

        // 비즈니스 로직 호출, 파라미터로 userId, teamId, request(bugetDate, amount) 넘겨준다.
        BudgetResponse response = budgetService.createDailyBudget(
                userId, teamId, request.budgetDate(), request.amount()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{budgetId}")
    public ResponseEntity<BudgetResponse> updateBudget(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long budgetId,
            @RequestBody UpdateBudgetRequest request) {

        BudgetResponse response = budgetService.updateBudget(userId, budgetId, request.amount());
        return ResponseEntity.ok(response);
    }
    )

    @GetMapping("/teams/{teamId}")
    public ResponseEntity<List<BudgetResponse>> getBudgetsByTeam(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long teamId) {
        List<BudgetResponse> response = budgetService.getBudgetsByTeam(userId, teamId);
        return ResponseEntity.ok(response);

    }
}

