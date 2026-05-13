package com.devcrew.togetherpay.domain.openChatAI.controller;

import com.devcrew.togetherpay.domain.openChatAI.dto.request.ChatRequest;
import com.devcrew.togetherpay.domain.openChatAI.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/openChatAI")
public class ChatController {

  private final ChatService chatService;

  @PostMapping("/teams/{teamId}/chat")
  public ResponseEntity<String> chat(
          @AuthenticationPrincipal Long userId,
          @PathVariable Long teamId,
          @Valid @RequestBody ChatRequest request) {

    return ResponseEntity.ok(chatService.chat(userId, teamId, request));
  }


}
