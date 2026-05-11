package com.devcrew.togetherpay.domain.openChatAI.controller;

import com.devcrew.togetherpay.domain.openChatAI.dto.request.ChatRequest;
import com.devcrew.togetherpay.domain.openChatAI.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController("/api/v1/openChatApi")
public class ChatController {

  private final ChatService chatService;

  @RequestMapping("/chat")
  public ResponseEntity<ChatResponse> chat(ChatRequest request) {

    return ResponseEntity.ok(chatService.chat(request));

  }


}
