package com.devcrew.togetherpay.domain.trip.controller;

import com.devcrew.togetherpay.domain.trip.dto.CreateTripRequest;
import com.devcrew.togetherpay.domain.trip.dto.TripResponse;
import com.devcrew.togetherpay.domain.trip.dto.UpdateTripRequest;
import com.devcrew.togetherpay.domain.trip.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/trips")
public class TripController {

    private final TripService tripService;

    /**
     * 여행 생성
     * @param userId
     * @param request
     * @return
     */
    @PostMapping
    public ResponseEntity<TripResponse> createTrip(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid CreateTripRequest request
    ) {
        TripResponse response = tripService.createTrip(userId, request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 여행 상세 조회
     * @param userId
     * @param tripId
     * @return
     */
    @GetMapping("{tripId}")
    public ResponseEntity<TripResponse> getTrip (
            @AuthenticationPrincipal Long userId,
            @PathVariable Long tripId
    ) {
        return ResponseEntity.ok(tripService.getTrip(userId, tripId));
    }

    /**
     * 팀별 여행 목록 조회
     * @param userId
     * @param teamId
     * @return
     */
    @GetMapping("teams/{teamId}")
    public ResponseEntity<List<TripResponse>> getTripsByTeam (
            @AuthenticationPrincipal Long userId,
            @PathVariable Long teamId
    ) {
        return ResponseEntity.ok(tripService.getTripsByTeam(userId, teamId));
    }

    /**
     * 여행 수정
     * @param userId
     * @param tripId
     * @param request
     * @return
     */
    @PatchMapping("/{tripId}")
    public ResponseEntity<TripResponse> updateTrip(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long tripId,
            @RequestBody @Valid UpdateTripRequest request
    ) {
        return ResponseEntity.ok(tripService.updateTrip(userId, tripId, request));
    }

    /**
     * 여행 삭제
     * @param userId
     * @param tripId
     * @return
     */
    @DeleteMapping("/{tripId}")
    public ResponseEntity<Void> deleteTrip(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long tripId
    ) {
        tripService.deleteTrip(userId, tripId);
        return ResponseEntity.noContent().build();
    }

}
