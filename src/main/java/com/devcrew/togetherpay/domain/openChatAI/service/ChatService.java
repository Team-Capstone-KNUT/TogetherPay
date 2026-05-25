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

      sb.append("\n번호를 입력헤주세요.");

      return sb.toString();
    }

  private String handlerTripSelection(Long userId, String question) {
      TripSelectionContext context = pendingSelections.get(userId);

      int selectedNumber;

      try {
        selectedNumber = Integer.parseInt(question.trim()); // 공백 제거.
      } catch (NumberFormatException e) {
        return "번호로 입력해주세요.";
      }

      // 1 보다 작거나 또는 tripIds 갯수를 넘어갈 때.
      if (selectedNumber < 1 || selectedNumber > context.tripIds().size()) {
        return "선택 가능한 번호가 아닙니다. 다시 입력해주세요.";
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

        Trip trip = schedule.getTrip();

        StringBuilder sb = new StringBuilder();
        sb.append("여행명: ")
                .append(trip.getTitle())
                .append("\n\n");

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
            너는 여행 정산 서비스 TogetherPay의 AI '루루'야.
            사용자가 등록한 여행 일정만 기반으로 여행을 요약해줘.
            없는 장소, 시간, 비용, 교통편은 절대 지어내지 마.
    
            단순히 일정을 반복하지 말고, 사용자가 실제로 도움이 된다고 느낄 정보를 정리해줘.
    
            반드시 아래 형식으로 답변해:
    
            1. 루루의 한눈에 보는 여행 흐름
            - 전체 일정이 어떤 흐름인지 2~4문장으로 설명해.
            - 도시 이동, 쇼핑, 식사, 휴식, 공항 일정 같은 큰 흐름을 짚어줘.
    
            2. 날짜별 일정 정리
            - 날짜별로 제목과 설명을 자연스럽게 요약해.
            - 각 날짜마다 "이 날의 포인트"를 한 줄로 덧붙여줘.
    
            3. 루루 체크
            - 일정상 사용자가 확인하면 좋을 점을 2~5개 알려줘.
            - 이동 수단, 숙소, 예약, 공항 도착 시간, 준비물, 예산 분배 같은 관점에서 봐줘.
            - 단, 등록된 일정에서 추론 가능한 범위 안에서만 말해.
    
            4. 빠진 정보
            - 일정에 시간, 장소 주소, 이동 수단, 예약 정보, 예산 정보가 부족해 보이면 알려줘.
            - 확실하지 않은 내용은 "~가 등록되어 있지 않다면 추가해두면 좋아요"처럼 표현해.
    
            답변 톤:
            - 친근하지만 너무 장난스럽지 않게.
            - 한국어로 답변해.
            - 너무 길지 않게, 모바일 화면에서 읽기 좋게.
            - 문단 사이를 띄워서 읽기 쉽게.
    
            등록된 일정:
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
    boolean hasScheduleKeyword =
            question.contains("일정")
                    || question.contains("여행 계획")
                    || question.contains("여행 일정");

    boolean hasSummaryKeyword =
            question.contains("요약")
            || question.contains("정리")
            || question.contains("확인")
            || question.contains("보여줘");

    return hasScheduleKeyword && hasSummaryKeyword;
  }

}
