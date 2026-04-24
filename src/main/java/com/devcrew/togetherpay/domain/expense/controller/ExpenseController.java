package com.devcrew.togetherpay.domain.expense.controller;

import com.devcrew.togetherpay.domain.expense.dto.FindDetailExpenseResponse;
import com.devcrew.togetherpay.domain.expense.dto.FindExpensesResponse;
import com.devcrew.togetherpay.domain.expense.dto.RegisterDutchExpenseRequest;
import com.devcrew.togetherpay.domain.expense.dto.RegisterIndividualExpenseRequest;
import com.devcrew.togetherpay.domain.expense.dto.UpdateExpenseRequest;
import com.devcrew.togetherpay.domain.expense.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/expense")
public class ExpenseController {

  private final ExpenseService expenseService;

  /**
   * 지출 등록(더치페이)
   * @param userId
   * @param request
   * @return
   */
  @PostMapping("/dutch")
  public ResponseEntity<Void> registerWithDutch(
      @AuthenticationPrincipal Long userId,
      @RequestBody @Valid RegisterDutchExpenseRequest request
  ) {

    expenseService.registerWithDutchPay(userId, request.toCommand());
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  /**
   * 개인 지출 등록
   * @param userId
   * @param request
   * @return
   */
  @PostMapping("/individual")
  public ResponseEntity<Void> registerWithIndividual(
      @AuthenticationPrincipal Long userId,
      @RequestBody @Valid RegisterIndividualExpenseRequest request
  ) {
    expenseService.registerWithIndividualAmount(userId, request.toCommand());
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  /**
   * 특정 지출 조회
   * @param userId
   * @param expenseId
   * @return
   */
  @GetMapping("/{expenseId}")
  public ResponseEntity<FindDetailExpenseResponse> getExpense(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long expenseId
  ) {
    FindDetailExpenseResponse response = expenseService.getExpense(userId, expenseId);
    return ResponseEntity.ok(response);
  }

  /**
   * 여행 단위의 지출 전체 조회
   * @param userId
   * @param tripId
   * @return
   */
  @GetMapping("/trip/{tripId}")
  public ResponseEntity<FindExpensesResponse> getExpenses(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long tripId
  ) {
    FindExpensesResponse response = expenseService.getExpenses(userId, tripId);
    return ResponseEntity.ok(response);
  }

  /**
   * 지출 수정
   * @param userId
   * @param expenseId
   * @param request
   * @return
   */
  @PatchMapping("/{expenseId}")
  public ResponseEntity<Void> update(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long expenseId,
      @RequestBody @Valid UpdateExpenseRequest request
  ) {
    expenseService.updateExpense(userId, expenseId, request.toCommand());
    return ResponseEntity.noContent().build();
  }

  /**
   * 지출 삭제
   * @param userId
   * @param expenseId
   * @return
   */
  @DeleteMapping("/{expenseId}")
  public ResponseEntity<Void> delete(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long expenseId
  ) {
    expenseService.deleteExpense(userId, expenseId);
    return ResponseEntity.noContent().build();
  }
}
