package com.devcrew.togetherpay.domain.openChatAI.controller;

import com.devcrew.togetherpay.domain.openChatAI.dto.request.ChatRequest;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.ChatResponse;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.PlaceDetailResponse;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.PlacePhotoMediaResponse;
import com.devcrew.togetherpay.domain.openChatAI.service.ChatService;
import com.devcrew.togetherpay.domain.openChatAI.service.GooglePlacesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/openChatAI")
public class ChatController {

  private final ChatService chatService;
  private final GooglePlacesService googlePlacesService;

  @PostMapping("/teams/{teamId}/chat")
  public ResponseEntity<ChatResponse> chat(
          @AuthenticationPrincipal Long userId,
          @PathVariable Long teamId,
          @Valid @RequestBody ChatRequest request) {

    return ResponseEntity.ok(chatService.chat(userId, teamId, request));
  }

  @GetMapping("/places/{placeId}")
  public ResponseEntity<PlaceDetailResponse> getPlaceDetail(
          @PathVariable String placeId,
          @RequestParam(required = false) OffsetDateTime visitDateTime) {

    return ResponseEntity.ok(googlePlacesService.getPlaceDetail(placeId, visitDateTime));
  }

  @GetMapping("/places/{placeId}/photos/{photoReference}/media")
  public ResponseEntity<PlacePhotoMediaResponse> getPlacePhotoMedia(
          @PathVariable String placeId,
          @PathVariable String photoReference,
          @RequestParam(defaultValue = "600") Integer maxWidthPx,
          @RequestParam(defaultValue = "400") Integer maxHeightPx) {

    return ResponseEntity.ok(googlePlacesService.getPlacePhotoMedia(placeId, photoReference, maxWidthPx, maxHeightPx));
  }

}
