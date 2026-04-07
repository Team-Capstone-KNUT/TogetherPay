package com.devcrew.togetherpay.global.common.ExchangeRate.service;

import com.devcrew.togetherpay.domain.expense.Currency;
import com.devcrew.togetherpay.global.common.ExchangeRate.dto.FindExchangeRateResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = false)
public class ExchangeRateService {

  @Value("${openApi.auth-key}")
  private String authkey;
  private final WebClient webClient;

  /**
   *
   * @param currency
   * @Param expenseDate
   * @return FindExchangeRateResponse
   * 환율 정보 조회
   */
  private FindExchangeRateResponse searchExchange(Currency currency, LocalDate expenseDate) {

    // 패턴 설정
    String searchDate = expenseDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

    return webClient.get()
        .uri(uriBuilder -> uriBuilder
            .queryParam("authkey", authkey)
            .queryParam("searchdate", searchDate)
            .queryParam("data", "AP01")
            .build())
        .retrieve()
        .bodyToFlux(FindExchangeRateResponse.class)
        .filter(response -> response.curUnit().contains(currency.name()))
        .blockFirst();

  }

  /**
   * 선택한 날짜의 환율 조회 (1 외화 → KRW 매매기준율)
   * KRW인 경우 null 반환
   *
   * 매매기준율로 반환.
   */
  public BigDecimal getExchangeRate(Currency currency, LocalDate expenseDate) {
    if (currency == Currency.KRW) {
      return null;
    }

    FindExchangeRateResponse response = searchExchange(currency, expenseDate);

    if (response == null || response.dealBasR() == null) {
      throw new IllegalStateException("환율 정보를 가져올 수 없습니다: " + currency);
    }

    // ',' 쉼표 제거
    String rateStr = response.dealBasR().replace(",", "");
    return new BigDecimal(rateStr);
  }

}

