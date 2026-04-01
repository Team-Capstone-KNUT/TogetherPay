package com.devcrew.togetherpay.global.common.ExchangeRate.service;

import com.devcrew.togetherpay.domain.expense.Currency;
import com.devcrew.togetherpay.global.common.ExchangeRate.dto.FindExchangeRateResponse;
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
   * @return FindExchangeRateResponse
   * 환율 정보 조회
   */
  public FindExchangeRateResponse getExchangeRate(Currency currency) {

    String searchDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

    return webClient.get()
        .uri(uriBuilder -> uriBuilder
            .queryParam("authkey", authkey)
            .queryParam("searchdate", searchDate)
            .queryParam("data", "AP01")
            .build())
        .retrieve()
        .bodyToFlux(FindExchangeRateResponse.class) // stream 처럼 파이프라인에 하나씩 흘려보내기. (Flux)
        .filter(response -> response.curUnit().contains(currency.name()))
        .blockFirst();

  }

}

