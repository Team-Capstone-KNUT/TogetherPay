package com.devcrew.togetherpay.domain.settlement.controller;

import com.devcrew.togetherpay.domain.settlement.dto.FindDetailSettlementResponse;
import com.devcrew.togetherpay.domain.settlement.dto.FindSettlementsResponse;
import com.devcrew.togetherpay.domain.settlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/settlement")
public class SettlementController {
  private final SettlementService settlementService;

  // 정산 요청 [결제자]
  @PostMapping("/{expenseId}")
  public ResponseEntity<Void> create(
      @AuthenticationPrincipal Long userId, // 오직 결제자만이 정산 요청을 할 수 있음.
      @PathVariable Long expenseId
  ) {
    settlementService.create(userId, expenseId);
    return ResponseEntity.noContent().build();
  }

  // 정산 상세 조회 [지출 참여자]
  @GetMapping("/detail/{settlementId}")
  public ResponseEntity<FindDetailSettlementResponse> getSettlement(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long settlementId
  ) {
    FindDetailSettlementResponse response = settlementService.getSettlement(userId, settlementId);
    return ResponseEntity.ok(response);
  }

  // 정산 목록 조회 [결제자 - expenseId 기준]
  @GetMapping("/expense/{expenseId}")
  public ResponseEntity<FindSettlementsResponse> getSettlements(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long expenseId
  ) {
    FindSettlementsResponse response = settlementService.getSettlements(userId, expenseId);
    return ResponseEntity.ok(response);
  }

  // 팀 정산 목록 조회 [팀 멤버]
  @GetMapping("/team/{teamId}")
  public ResponseEntity<FindSettlementsResponse> getTeamSettlements(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long teamId
  ) {
    FindSettlementsResponse response = settlementService.getTeamSettlements(userId, teamId);
    return ResponseEntity.ok(response);
  }

  // 내 정산 목록 조회 [지출 참여자]
  @GetMapping("/my")
  public ResponseEntity<FindSettlementsResponse> getMySettlements(
      @AuthenticationPrincipal Long userId
  ) {
    FindSettlementsResponse response = settlementService.getMySettlements(userId);
    return ResponseEntity.ok(response);
  }


}
