package com.devcrew.togetherpay.domain.openChatAI.service;

import com.devcrew.togetherpay.domain.openChatAI.dto.request.ChatRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

  private final ChatClient chatClient;

  public String chat(Long userId, ChatRequest request) {

    String question = request.question();

    if (isTravelScheduleSummary(question)) {
      // 여행 일정 조회

      // 여행 일정을 포맷하기. (StringBuilder)
      StringBuilder sb = new StringBuilder();

      // 일정 넘기기.
//      return chatClient.prompt()
//              .user() // 일정 삽입.
//              .call()
//              .content();
    }

      // 옵션 값은 ChatConfig에서 설정함.
      return chatClient.prompt()
              .user(request.question())
              .call()
              .content();
    }

  // 여행 일정 요약 질문 판단하기.
  // 특정 키워드가 있으면 호출.
  private boolean isTravelScheduleSummary(String question) {
    boolean hasMyKeyword =
            question.contains("내")
            || question.contains("나의")
            || question.contains("등록한")
            || question.contains("저장한");

    boolean hasScheduleKeyword =
            question.contains("일정")
                    || question.contains("여행 계획")
                    || question.contains("여행 일정");

    boolean hasSummaryKeyword =
            question.contains("요약")
            || question.contains("정리")
            || question.contains("확인")
            || question.contains("보여줘");

    return hasMyKeyword && hasScheduleKeyword && hasSummaryKeyword;
  }

}
