package com.devcrew.togetherpay.global.common.ExchangeRate.controller;

import com.devcrew.togetherpay.domain.expense.Currency;
import com.devcrew.togetherpay.global.common.ExchangeRate.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
public class ExchangeRateController {
    private final ExchangeRateService exchangeRateService;

    @GetMapping("/api/v1/exchange-rates")
    public ResponseEntity<BigDecimal> getCurrentRate(
            @RequestParam Currency currency,
            @RequestParam(required = false) String date // yyyyMMdd
    ) {
        LocalDate targetDate = (date != null) ? LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd")) : LocalDate.now();
        BigDecimal rate = exchangeRateService.getExchangeRate(currency, targetDate);
        return ResponseEntity.ok(rate);
    }
}
