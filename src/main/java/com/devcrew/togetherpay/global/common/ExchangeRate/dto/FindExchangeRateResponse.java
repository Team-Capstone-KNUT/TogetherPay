package com.devcrew.togetherpay.global.common.ExchangeRate.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FindExchangeRateResponse(
    Integer result,
    @JsonProperty("cur_unit") // 통화 코드
    String curUnit,
    String ttb, // 송금 받을 때, 환율
    String tts, // 송금 보낼 때, 환율
    @JsonProperty("deal_bas_r") String dealBasR // 매매기준율 (중간값) = (ttb + tts) / 2
) {

}
