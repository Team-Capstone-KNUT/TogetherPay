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

  @PostMapping("/dutch")
  public ResponseEntity<Void> registerWithDutch(
      @LoginUser Long userId,
      @RequestBody @Valid RegisterDutchExpenseRequest request
  ) {

    expenseService.registerWithDutchPay(userId, request.toCommand());
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @PostMapping("/individual")
  public ResponseEntity<Void> registerWithIndividual(
      @LoginUser Long userId,
      @RequestBody @Valid RegisterIndividualExpenseRequest request
  ) {
    expenseService.registerWithIndividualAmount(userId, request.toCommand());
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @GetMapping("/{expenseId}")
  public ResponseEntity<FindDetailExpenseResponse> getExpense(
      @LoginUser Long userId,
      @PathVariable Long expenseId
  ) {
    FindDetailExpenseResponse response = expenseService.getExpense(userId, expenseId);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{teamId}")
  public ResponseEntity<FindExpensesResponse> getExpenses(
      @LoginUser Long userId,
      @PathVariable Long teamId
  ) {
    FindExpensesResponse response = expenseService.getExpenses(userId, teamId);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{expenseId}")
  public ResponseEntity<Void> update(
      @LoginUser Long userId,
      @PathVariable Long expenseId,
      @RequestBody @Valid UpdateExpenseRequest request
  ) {
    expenseService.updateExpense(userId, expenseId, request.toCommand());
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{expenseId}")
  public ResponseEntity<Void> delete(
      @LoginUser Long userId,
      @PathVariable Long expenseId
  ) {
    expenseService.deleteExpense(userId, expenseId);
    return ResponseEntity.noContent().build();
  }

}
