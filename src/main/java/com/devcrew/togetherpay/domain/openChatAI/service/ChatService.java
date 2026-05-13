package com.devcrew.togetherpay.domain.openChatAI.service;

import com.devcrew.togetherpay.domain.openChatAI.TripSelectionContext;
import com.devcrew.togetherpay.domain.openChatAI.dto.request.ChatRequest;
import com.devcrew.togetherpay.domain.schedule.Schedule;
import com.devcrew.togetherpay.domain.schedule.repository.ScheduleRepository;
import com.devcrew.togetherpay.domain.trip.Trip;
import com.devcrew.togetherpay.domain.trip.repository.TripRepository;
import com.devcrew.togetherpay.global.error.ErrorCode;
import com.devcrew.togetherpay.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatService {

  private final ChatClient chatClient;
  private final TripRepository tripRepository;
  private final ScheduleRepository scheduleRepository;

  private final Map<Long, TripSelectionContext> pendingSelections = new HashMap<>();

  public String chat(Long userId, Long teamId, ChatRequest request) {
    String question = request.question();

    // 사용자가 이전에 여행 선택 대기 상태였는지 확인
    if (pendingSelections.containsKey(userId)) {
      return handlerTripSelection(userId, question);
    }

    // 일정 요약 요청을 한 경우.
    if (isTravelScheduleSummary(question)) {
      return askTripSelection(userId, teamId);
    }

    // 일반 여행 질문.
    return chatClient.prompt()
            .user(request.question())
            .call()
            .content();
    }

    private String askTripSelection(Long userId, Long teamId) {
      List<Trip> trips = tripRepository.findByTeamIdOrderByStartDateAsc(teamId);

      if (trips.isEmpty()) {
        return "등록된 여행이 없습니다.";
      }

      List<Long> tripIds = trips.stream()
              .map(Trip::getId)
              .toList();

      pendingSelections.put(userId, new TripSelectionContext(teamId, tripIds));

      StringBuilder sb = new StringBuilder();
      sb.append("요약할 여행을 선택해주세요.\n\n");

      int i = 0;
      while(i < trips.size()) {
        Trip trip = trips.get(i);

        sb.append(++i)
                .append(". ")
                .append(trip.getTitle())
                .append(" (id ")
                .append(trip.getId())
                .append(")")
                .append("\n");
      }

      sb.append("\n번호를 입력헤주세요. >>> ");

      return sb.toString();
    }

  private String handlerTripSelection(Long userId, String question) {
      TripSelectionContext context = pendingSelections.get(userId);

      int selectedNumber;

      try {
        selectedNumber = Integer.parseInt(question.trim()); // 공백 제거.
      } catch (NumberFormatException e) {
        return "번호로 입력해주세요. >>> ";
      }

      // 1 보다 작거나 또는 tripIds 갯수를 넘어갈 때.
      if (selectedNumber < 1 || selectedNumber > context.tripIds().size()) {
        return "선택 가능한 번호가 아닙니다. 다시 입력해주세요. >>> ";
      }

      // index 이기 때문에 -1 해줌. (0 1 2 3 ~)
      Long selectedTripId = context.tripIds().get(selectedNumber -1);

      pendingSelections.remove(userId);

      return summarizeSchedule(selectedTripId);
    }

    // 여행 일정 요약
    private String summarizeSchedule(long tripId) {
      Schedule schedule = scheduleRepository.findByTripId(tripId)
              .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

      StringBuilder sb = new StringBuilder();

      sb.append("사용자가 등록한 여행 일정입니다.\n\n");

      schedule.getScheduleItems()
              .forEach(item -> {
                sb.append("[")
                        .append(item.getDate())
                        .append("]\n");

                sb.append("제목: ")
                        .append(item.getTitle() == null ? "없음" : item.getTitle())
                        .append("\n");

                sb.append("설명: ")
                        .append(item.getDescription() == null ? "없음" : item.getDescription())
                        .append("\n\n");
              });

      String prompt = """
              아래 여행 일정만 기반으로 요약해주세요.
              없는 내용은 지어내지 마세요.
              날짜순으로 정리하고, 전체 여행 흐름을 보기 쉽게 설명해주세요.
              
              %s
              """.formatted(sb.toString()); // SpringBuilder 넣었던 일정 toString()으로 반환

      return chatClient.prompt()
              .user(prompt)
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
