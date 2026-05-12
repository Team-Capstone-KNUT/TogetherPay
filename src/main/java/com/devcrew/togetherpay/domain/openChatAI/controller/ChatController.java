package com.devcrew.togetherpay.domain.openChatAI.controller;

import com.devcrew.togetherpay.domain.openChatAI.dto.request.ChatRequest;
import com.devcrew.togetherpay.domain.openChatAI.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/openChatAI")
public class ChatController {

  private final ChatService chatService;

  @PostMapping("/chat")
  public ResponseEntity<String> chat(
          @AuthenticationPrincipal Long userId,
          @Valid @RequestBody ChatRequest request) {

    return ResponseEntity.ok(chatService.chat(userId, request));
  }


}
