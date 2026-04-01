package com.devcrew.togetherpay.global.common.ExchangeRate.controller;

import com.devcrew.togetherpay.domain.expense.Currency;
import com.devcrew.togetherpay.global.common.ExchangeRate.service.ExchangeRateService;
import com.devcrew.togetherpay.global.common.ExchangeRate.dto.FindExchangeRateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/exchange-rate")
public class ExchangeRateController {

  private final ExchangeRateService exchangeRateService;

  @GetMapping("{currency}")
  public ResponseEntity<FindExchangeRateResponse> getExchangeRate(
      @PathVariable Currency currency
  ) {
    FindExchangeRateResponse response = exchangeRateService.getExchangeRate(currency);
    return ResponseEntity.ok(response);

  }

}
