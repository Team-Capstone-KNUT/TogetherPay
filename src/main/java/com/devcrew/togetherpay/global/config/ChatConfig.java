package com.devcrew.togetherpay.global.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatConfig {

  @Bean
  public ChatClient chatClient(ChatClient.Builder builder) {
    return builder
        .defaultSystem("당신은 여행 정보를 제공하는 AI입니다. 사용자가 여행에 대해서 질문하면 답하세요. "
            + "단, 여행에 상관없는 질문이면 '답변 불가'라고 답하세요.")
        .defaultOptions(ChatOptions.builder()
            .temperature(0.7) // 높을수록 창의적임. 같은 답을 최소화함.
            .maxTokens(1000) // 최대 길이 제한.
            .build())
        .build();
  }

}
