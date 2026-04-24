package com.devcrew.togetherpay.domain.settlement.dto;

import com.devcrew.togetherpay.domain.settlement.Settlement;
import java.util.List;

public record FindSettlementsResponse(
    List<FindSettlementResponse> findSettlementResponses
) {

  public static FindSettlementsResponse of(List<Settlement> settlements, Long myUserId) {
    return new FindSettlementsResponse(
        settlements.stream()
            .map(s -> FindSettlementResponse.of(s, myUserId)) // 개별 DTO 변환 시에 userId 넘겨줌
            .toList()
    );
  }
}
