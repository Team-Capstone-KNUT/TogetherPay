package com.devcrew.togetherpay.domain.schedule.controller;

import com.devcrew.togetherpay.domain.schedule.dto.request.CreateScheduleRequest;
import com.devcrew.togetherpay.domain.schedule.dto.response.ItemDetailResponse;
import com.devcrew.togetherpay.domain.schedule.dto.response.ScheduleItemsResponse;
import com.devcrew.togetherpay.domain.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    // 여행 일정 생성
    @PostMapping
    public ResponseEntity<Void> create(
            @AuthenticationPrincipal Long userId,
            @RequestBody CreateScheduleRequest request
    ) {
        scheduleService.create(userId, request.toCommand());

        return ResponseEntity.status(HttpStatus.CREATED).build();

    }

    // 여행 단위로 조회
    @GetMapping("/trip/{tripId}")
    public ResponseEntity<ScheduleItemsResponse> getScheduleItems(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long tripId
    ) {
        ScheduleItemsResponse response = scheduleService.getScheduleItems(userId, tripId);
        return ResponseEntity.ok(response);
    }

    // 일정 상세 조회
    @GetMapping("/{itemId}")
    public ResponseEntity<ItemDetailResponse> getScheduleItem(
            @AuthenticationPrincipal Long userId,
            @PathVariable(value = "itemId") Long scheduleItemId
    ) {
        ItemDetailResponse response = scheduleService.getScheduleItem(scheduleItemId);
        return ResponseEntity.ok(response);
    }

    // 일정 수정&등록
    @PatchMapping("/{itemId}")
    public ResponseEntity<Void> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable(value = "itemId") Long scheduleItemId
    ) {



    }




}
