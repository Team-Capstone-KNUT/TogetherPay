package com.devcrew.togetherpay.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

  @Bean
  public WebClient webClient(WebClient.Builder builder) {
    return builder
        .baseUrl("https://oapi.koreaexim.go.kr/site/program/financial/exchangeJSON")
        .defaultHeader("Content-Type", "application/json")
        .build();
  }

}
