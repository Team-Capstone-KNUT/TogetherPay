package com.devcrew.togetherpay.domain.openChatAI.service;

import com.devcrew.togetherpay.domain.openChatAI.dto.request.ChatRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

  private final ChatClient chatClient;

  public ChatResponse chat(ChatRequest reqeust) {

    // 옵션 값은 ChatConfig에서 설정함.
    ChatResponse response = chatClient.prompt()
        .user(reqeust.question())
        .call()
        .chatResponse();

    return response;
  }

}
