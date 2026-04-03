package com.devcrew.togetherpay.domain.settlement.dto;

import com.devcrew.togetherpay.domain.settlement.Settlement;
import java.util.List;

public record FindSettlementsResponse(
    List<FindSettlementResponse> findSettlementResponses
) {

  public static FindSettlementsResponse of(List<Settlement> settlements) {
    return new FindSettlementsResponse(
        settlements.stream()
            .map(FindSettlementResponse::of)
            .toList()
    );
  }
}
