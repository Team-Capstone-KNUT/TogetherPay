package com.devcrew.togetherpay.domain.openChatAI.service;

import com.devcrew.togetherpay.domain.budget.Budget;
import com.devcrew.togetherpay.domain.budget.repository.BudgetRepository;
import com.devcrew.togetherpay.domain.expense.dto.ExpenseCategoryTotal;
import com.devcrew.togetherpay.domain.expense.repository.ExpenseRepository;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.ChatBlock;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.ChatResponse;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.TripHealthImprovementOption;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.TripHealthImprovementResponse;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.TripHealthImprovementStep;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.TripHealthScoreResponse;
import com.devcrew.togetherpay.domain.schedule.Schedule;
import com.devcrew.togetherpay.domain.schedule.ScheduleItem;
import com.devcrew.togetherpay.domain.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripHealthScoreService {

    private static final int BASE_SCORE = 100;

    private final ScheduleRepository scheduleRepository;
    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final ChatClient chatClient;

    @Transactional(readOnly = true)
    public ChatResponse analyze(long tripId, String question) {
        TripHealthScoreResponse score = calculateScore(tripId);
        String message = buildMessage(question, score);

        return new ChatResponse(
                message,
                List.of(new ChatBlock("trip_health_score", score))
        );
    }

    @Transactional(readOnly = true)
    public ChatResponse improve(long tripId, String question, int targetScore) {
        TripHealthScoreResponse score = calculateScore(tripId);
        int normalizedTargetScore = normalizeTargetScore(targetScore);
        TripHealthImprovementResponse improvement = buildImprovement(score, normalizedTargetScore);
        String message = buildImprovementMessage(question, improvement);

        return new ChatResponse(
                message,
                List.of(new ChatBlock("trip_health_improvement", improvement))
        );
    }

    private TripHealthScoreResponse calculateScore(long tripId) {
        ScoreAccumulator accumulator = new ScoreAccumulator();
        Optional<Schedule> schedule = scheduleRepository.findByTripId(tripId);

        if (schedule.isEmpty() || schedule.get().getScheduleItems().isEmpty()) {
            accumulator.penalize(40, "등록된 일정 정보가 부족해 여행 흐름을 판단하기 어렵습니다.");
        } else {
            analyzeSchedule(schedule.get(), accumulator);
        }

        analyzeBudget(tripId, accumulator);
        analyzeExpenseCategories(tripId, accumulator);

        int score = Math.max(0, Math.min(BASE_SCORE, accumulator.score()));
        return new TripHealthScoreResponse(
                tripId,
                score,
                resolveGrade(score),
                accumulator.reasons(),
                List.of(
                        new TripHealthImprovementOption(80, "80점 만들기"),
                        new TripHealthImprovementOption(90, "90점 만들기"),
                        new TripHealthImprovementOption(100, "100점 만들기")
                )
        );
    }

    private void analyzeSchedule(Schedule schedule, ScoreAccumulator accumulator) {
        List<ScheduleItem> items = schedule.getScheduleItems();
        long activeItemCount = items.stream().filter(this::hasAnyContent).count();
        long emptyItemCount = items.size() - activeItemCount;
        long weakDescriptionCount = items.stream()
                .filter(this::hasAnyContent)
                .filter(this::hasWeakDescription)
                .count();

        if (activeItemCount == 0) {
            accumulator.penalize(35, "일정 날짜는 있지만 실제 일정 내용이 거의 입력되어 있지 않습니다.");
            return;
        }

        if (emptyItemCount * 100 >= items.size() * 50L) {
            accumulator.penalize(25, "여행 날짜 중 절반 이상에 일정 내용이 비어 있습니다.");
        } else if (emptyItemCount * 100 >= items.size() * 30L) {
            accumulator.penalize(15, "일부 일정에 제목이나 설명이 부족해 준비 상태가 낮아 보입니다.");
        } else if (emptyItemCount > 0) {
            accumulator.penalize(5, "일부 날짜에 일정 내용이 비어 있습니다.");
        }

        if (weakDescriptionCount * 100 >= activeItemCount * 50L) {
            accumulator.penalize(15, "입력된 일정 설명이 짧아 장소, 이동, 예약 정보를 판단하기 어렵습니다.");
        } else if (weakDescriptionCount > 0) {
            accumulator.penalize(5, "일부 일정 설명이 짧아 준비 정보가 부족합니다.");
        }

        Map<LocalDate, Long> activeCountByDate = items.stream()
                .filter(this::hasAnyContent)
                .collect(Collectors.groupingBy(ScheduleItem::getDate, Collectors.counting()));

        long maxDailyCount = activeCountByDate.values().stream()
                .max(Comparator.naturalOrder())
                .orElse(0L);

        if (maxDailyCount > 7) {
            accumulator.penalize(15, "하루 일정이 과도하게 몰린 날짜가 있어 피로도가 높아질 수 있습니다.");
        } else if (maxDailyCount > 5) {
            accumulator.penalize(10, "하루 일정이 다소 빽빽한 날짜가 있습니다.");
        }

        double averageDailyCount = activeCountByDate.values().stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
        if (maxDailyCount >= 3 && averageDailyCount > 0 && maxDailyCount >= averageDailyCount * 2) {
            accumulator.penalize(10, "특정 날짜에 일정이 몰려 있어 날짜별 분산이 아쉽습니다.");
        }

        if (items.stream().noneMatch(this::hasAlternativeKeyword)) {
            accumulator.penalize(10, "우천이나 변수에 대비한 대체 일정 정보가 부족합니다.");
        }
    }

    private void analyzeBudget(long tripId, ScoreAccumulator accumulator) {
        Budget budget = budgetRepository.findByTripId(tripId).orElse(null);
        if (budget == null) {
            accumulator.penalize(10, "여행 예산이 설정되어 있지 않아 비용 리스크를 판단하기 어렵습니다.");
            return;
        }

        BigDecimal total = budget.getTotalAmount().getAmount();
        BigDecimal remaining = budget.getRemainingAmount().getAmount();
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            accumulator.penalize(20, "예산을 초과해 지출 관리 리스크가 큽니다.");
            return;
        }

        if (total.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal usageRate = total.subtract(remaining)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(total, 1, RoundingMode.HALF_UP);
            if (usageRate.compareTo(BigDecimal.valueOf(80)) >= 0) {
                accumulator.penalize(10, "예산 사용률이 80% 이상이라 남은 일정의 지출 관리가 필요합니다.");
            }
        }
    }

    private void analyzeExpenseCategories(long tripId, ScoreAccumulator accumulator) {
        List<ExpenseCategoryTotal> categories = expenseRepository.sumTotalAmountByTripIdGroupByCategory(tripId);
        if (categories.isEmpty()) {
            accumulator.penalize(5, "등록된 지출이 없어 실제 소비 흐름을 판단하기 어렵습니다.");
            return;
        }

        if (categories.size() <= 2) {
            accumulator.penalize(10, "지출 카테고리가 적어 여행 경험이 한쪽으로 치우쳐 보일 수 있습니다.");
        }
    }

    private String buildMessage(String question, TripHealthScoreResponse score) {
        String prompt = """
                너는 여행 정산 서비스 TogetherPay의 AI '루루'야.
                아래 서버 rule 기반 여행 건강도 점수를 사용자에게 설명해.
                
                규칙:
                - 점수와 사유는 제공된 데이터만 사용해.
                - 이동거리, 영업시간, 장소 좌표처럼 제공되지 않은 정보는 지어내지 마.
                - 3~5문장으로 모바일에서 읽기 좋게 답변해.
                - 한국어로 답변해.
                
                사용자 질문:
                %s
                
                점수 데이터:
                tripId: %s
                score: %s
                grade: %s
                reasons: %s
                """.formatted(question, score.tripId(), score.score(), score.grade(), score.reasons());

        try {
            String answer = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            if (answer != null && !answer.isBlank()) {
                return answer;
            }
        } catch (RuntimeException e) {
            log.warn("OpenAI trip health score failed. tripId={}, message={}", score.tripId(), e.getMessage());
        }

        return "이번 여행의 건강도는 %d점입니다. %s"
                .formatted(score.score(), String.join(" ", score.reasons()));
    }

    private TripHealthImprovementResponse buildImprovement(TripHealthScoreResponse score, int targetScore) {
        int requiredGain = Math.max(0, targetScore - score.score());
        List<TripHealthImprovementStep> candidateSteps = score.reasons().stream()
                .filter(reason -> !reason.equals("일정과 예산 흐름이 전반적으로 안정적입니다."))
                .map(this::toImprovementStep)
                .toList();

        List<TripHealthImprovementStep> steps = selectImprovementSteps(candidateSteps, requiredGain);
        if (steps.isEmpty()) {
            steps = List.of(new TripHealthImprovementStep(
                    "일정 디테일 보강",
                    "장소 주소, 예약 여부, 대체 일정, 예상 지출을 일정 설명에 추가하면 점수 신뢰도가 올라갑니다.",
                    5
            ));
        }

        int expectedGain = steps.stream()
                .mapToInt(TripHealthImprovementStep::expectedGain)
                .sum();
        int projectedScore = Math.min(targetScore, score.score() + expectedGain);

        return new TripHealthImprovementResponse(
                score.tripId(),
                score.score(),
                targetScore,
                projectedScore,
                expectedGain,
                score.grade(),
                resolveGrade(projectedScore),
                score.reasons(),
                steps
        );
    }

    private List<TripHealthImprovementStep> selectImprovementSteps(
            List<TripHealthImprovementStep> candidates,
            int requiredGain
    ) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        if (requiredGain <= 0) {
            return candidates.stream()
                    .limit(1)
                    .toList();
        }

        List<TripHealthImprovementStep> selected = new ArrayList<>();
        int expectedGain = 0;

        for (TripHealthImprovementStep candidate : candidates) {
            selected.add(candidate);
            expectedGain += candidate.expectedGain();

            if (expectedGain >= requiredGain) {
                break;
            }
        }

        return selected;
    }

    private TripHealthImprovementStep toImprovementStep(String reason) {
        if (reason.contains("일정 정보")) {
            return new TripHealthImprovementStep(
                    "일정 정보 입력",
                    "비어 있는 날짜에 핵심 일정 제목과 설명을 추가해 여행 흐름을 명확히 만드세요.",
                    15
            );
        }
        if (reason.contains("제목이나 설명")) {
            return new TripHealthImprovementStep(
                    "일정 설명 보강",
                    "제목만 있는 일정에는 목적, 예약 여부, 준비물을 한 줄씩 추가하세요.",
                    10
            );
        }
        if (reason.contains("비어")) {
            return new TripHealthImprovementStep(
                    "빈 날짜 일정 채우기",
                    "비어 있는 날짜에 식사, 이동, 휴식, 주요 방문지 중 최소 2개 이상을 입력하세요.",
                    15
            );
        }
        if (reason.contains("설명이 짧")) {
            return new TripHealthImprovementStep(
                    "일정 설명 구체화",
                    "각 일정 설명에 장소, 예상 시간, 이동 수단, 예약 여부를 추가하세요.",
                    10
            );
        }
        if (reason.contains("빽빽") || reason.contains("과도하게")) {
            return new TripHealthImprovementStep(
                    "하루 일정 수 줄이기",
                    "가장 빽빽한 날짜의 우선순위 낮은 일정을 다른 날로 옮기거나 휴식 시간을 추가하세요.",
                    10
            );
        }
        if (reason.contains("몰려")) {
            return new TripHealthImprovementStep(
                    "날짜별 일정 분산",
                    "특정 날짜에 몰린 일정을 여유 있는 날짜로 분산해 피로도를 낮추세요.",
                    10
            );
        }
        if (reason.contains("대체 일정")) {
            return new TripHealthImprovementStep(
                    "대체 일정 추가",
                    "우천, 폐점, 컨디션 저하에 대비해 실내 명소나 숙소 근처 후보를 일정 설명에 추가하세요.",
                    5
            );
        }
        if (reason.contains("예산")) {
            return new TripHealthImprovementStep(
                    "예산 리스크 줄이기",
                    "남은 일정의 식비와 쇼핑 예산 상한을 정하고, 고정비와 선택 지출을 분리하세요.",
                    10
            );
        }
        if (reason.contains("카테고리")) {
            return new TripHealthImprovementStep(
                    "경험 다양성 보강",
                    "식사, 관광, 휴식, 쇼핑 지출이 한쪽으로 치우치지 않도록 일정 경험을 섞어보세요.",
                    5
            );
        }
        if (reason.contains("지출이 없어")) {
            return new TripHealthImprovementStep(
                    "지출 기록 추가",
                    "식비, 교통비, 숙박비처럼 이미 발생한 비용을 등록해 예산 흐름을 확인하세요.",
                    5
            );
        }

        return new TripHealthImprovementStep(
                "여행 계획 보강",
                reason,
                5
        );
    }

    private String buildImprovementMessage(String question, TripHealthImprovementResponse improvement) {
        String prompt = """
                너는 여행 정산 서비스 TogetherPay의 AI '루루'야.
                아래 서버 rule 기반 여행 건강도 개선안을 사용자에게 설명해.
                
                규칙:
                - targetScore를 반드시 보장한다고 말하지 마. "가까워지려면"으로 표현해.
                - 제공된 개선 steps만 사용해.
                - 이동거리, 영업시간, 장소 좌표처럼 제공되지 않은 정보는 지어내지 마.
                - 3~5문장으로 모바일에서 읽기 좋게 답변해.
                - 한국어로 답변해.
                
                사용자 질문:
                %s
                
                개선 데이터:
                tripId: %s
                currentScore: %s
                targetScore: %s
                projectedScore: %s
                expectedGain: %s
                currentGrade: %s
                targetGrade: %s
                focusAreas: %s
                steps: %s
                """.formatted(
                question,
                improvement.tripId(),
                improvement.currentScore(),
                improvement.targetScore(),
                improvement.projectedScore(),
                improvement.expectedGain(),
                improvement.currentGrade(),
                improvement.targetGrade(),
                improvement.focusAreas(),
                improvement.steps()
        );

        try {
            String answer = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            if (answer != null && !answer.isBlank()) {
                return answer;
            }
        } catch (RuntimeException e) {
            log.warn("OpenAI trip health improvement failed. tripId={}, targetScore={}, message={}",
                    improvement.tripId(), improvement.targetScore(), e.getMessage());
        }

        return "현재 %d점에서 %d점에 가까워지려면 %s"
                .formatted(
                        improvement.currentScore(),
                        improvement.targetScore(),
                        improvement.steps().stream()
                                .map(TripHealthImprovementStep::title)
                                .collect(Collectors.joining(", "))
                );
    }

    private int normalizeTargetScore(int targetScore) {
        if (targetScore >= 100) {
            return 100;
        }
        if (targetScore >= 90) {
            return 90;
        }
        return 80;
    }

    private boolean hasAnyContent(ScheduleItem item) {
        return !isBlank(item.getTitle()) || !isBlank(item.getDescription());
    }

    private boolean hasWeakDescription(ScheduleItem item) {
        if (isBlank(item.getTitle()) || isBlank(item.getDescription())) {
            return true;
        }
        return item.getDescription().trim().length() < 20;
    }

    private boolean hasAlternativeKeyword(ScheduleItem item) {
        String text = ((item.getTitle() == null ? "" : item.getTitle()) + " " +
                (item.getDescription() == null ? "" : item.getDescription())).toLowerCase();
        return text.contains("대체")
                || text.contains("우천")
                || text.contains("비상")
                || text.contains("플랜b")
                || text.contains("plan b");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String resolveGrade(int score) {
        if (score >= 90) {
            return "EXCELLENT";
        }
        if (score >= 80) {
            return "GOOD";
        }
        if (score >= 60) {
            return "WARNING";
        }
        return "RISKY";
    }

    private static class ScoreAccumulator {
        private int score = BASE_SCORE;
        private final List<String> reasons = new ArrayList<>();

        private void penalize(int points, String reason) {
            score -= points;
            reasons.add(reason);
        }

        private int score() {
            return score;
        }

        private List<String> reasons() {
            if (reasons.isEmpty()) {
                return List.of("일정과 예산 흐름이 전반적으로 안정적입니다.");
            }
            return List.copyOf(reasons);
        }
    }
}
