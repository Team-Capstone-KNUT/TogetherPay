package com.devcrew.togetherpay.global.common.ExchangeRate.service;

import com.devcrew.togetherpay.domain.expense.Currency;
import com.devcrew.togetherpay.global.common.ExchangeRate.dto.FindExchangeRateResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    // 데이터가 없는 날짜(주말 등)를 대비해 재귀적 Fallback 로직 호출
    return searchWithFallback(currency, expenseDate, 0);
  }

  /**
   * 데이터가 없을 경우 최대 7일 전까지 역추적하여 환율 조회
   */
  private FindExchangeRateResponse searchWithFallback(Currency currency, LocalDate date, int depth) {
    // 일주일 넘게 데이터가 없다면 API 키나 시스템 문제로 판단하고 중단
    if (depth > 7) {
      return null;
    }

    // 패턴 설정
    String searchDate = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

    FindExchangeRateResponse response = webClient.get()
            .uri(uriBuilder -> uriBuilder
                    .queryParam("authkey", authkey)
                    .queryParam("searchdate", searchDate)
                    .queryParam("data", "AP01")
                    .build())
            .retrieve()
            .bodyToFlux(FindExchangeRateResponse.class)
            .filter(res -> res.curUnit().contains(currency.name()))
            .blockFirst();

    // ⭐️ 결과가 없으면 하루 전(minusDays(1)) 날짜로 다시 시도
    if (response == null) {
      return searchWithFallback(currency, date.minusDays(1), depth + 1);
    }

    return response;
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
    BigDecimal rate = new BigDecimal(rateStr);

    if (currency == Currency.JPY) {
      BigDecimal JPYToKRW = rate.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
      return JPYToKRW;
    }

    return rate;
  }

}
