package com.devcrew.togetherpay.domain.settlement.controller;

import com.devcrew.togetherpay.domain.settlement.dto.FindDetailSettlementResponse;
import com.devcrew.togetherpay.domain.settlement.dto.FindSettlementsResponse;
import com.devcrew.togetherpay.domain.settlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  /**
   * 송금 완료 상태 변경 [돈 보내는 사람 - Sender]
   */
  @PatchMapping("/{settlementId}/complete")
  public ResponseEntity<Void> completeTransfer(
          @AuthenticationPrincipal Long userId,
          @PathVariable Long settlementId
  ) {
    settlementService.updateTransferStatus(userId, settlementId);
    return ResponseEntity.ok().build();
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

  // 여행 정산 목록 조회 [여행 멤버]
  @GetMapping("/trips/{tripId}")
  public ResponseEntity<FindSettlementsResponse> getTripSettlements(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long tripId
  ) {
    FindSettlementsResponse response = settlementService.getTripSettlements(userId, tripId);
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
