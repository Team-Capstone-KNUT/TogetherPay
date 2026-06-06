package com.devcrew.togetherpay.domain.openChatAI.service;

import com.devcrew.togetherpay.domain.budget.Budget;
import com.devcrew.togetherpay.domain.budget.repository.BudgetRepository;
import com.devcrew.togetherpay.domain.expense.dto.ExpenseCategoryTotal;
import com.devcrew.togetherpay.domain.expense.repository.ExpenseRepository;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.BudgetCategoryInsight;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.BudgetInsightResponse;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.ChatBlock;
import com.devcrew.togetherpay.domain.openChatAI.dto.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetInsightService {

    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final ChatClient chatClient;

    @Transactional(readOnly = true)
    public ChatResponse analyze(long tripId, String question) {
        BudgetInsightResponse insight = buildInsight(tripId);
        String message = buildInsightMessage(question, insight);

        return new ChatResponse(
                message,
                List.of(new ChatBlock("budget_insight", insight))
        );
    }

    private BudgetInsightResponse buildInsight(long tripId) {
        BigDecimal expenseTotalAmount = expenseRepository.sumTotalAmountByTripId(tripId);
        List<BudgetCategoryInsight> topCategories = expenseRepository.sumTotalAmountByTripIdGroupByCategory(tripId).stream()
                .limit(3)
                .map(this::toCategoryInsight)
                .toList();

        Budget budget = budgetRepository.findByTripId(tripId).orElse(null);
        if (budget == null) {
            return new BudgetInsightResponse(
                    tripId,
                    null,
                    null,
                    toLongAmount(expenseTotalAmount),
                    null,
                    0.0,
                    "NO_BUDGET",
                    topCategories
            );
        }

        Long totalBudget = toLongAmount(budget.getTotalAmount().getAmount());
        Long remainingAmount = toLongAmount(budget.getRemainingAmount().getAmount());
        Long spentAmount = totalBudget - remainingAmount;
        double usageRate = calculateUsageRate(totalBudget, spentAmount);

        return new BudgetInsightResponse(
                tripId,
                budget.getTrip().getBaseCurrency().name(),
                totalBudget,
                spentAmount,
                remainingAmount,
                usageRate,
                resolveStatus(totalBudget, remainingAmount, usageRate),
                topCategories
        );
    }

    private BudgetCategoryInsight toCategoryInsight(ExpenseCategoryTotal total) {
        return new BudgetCategoryInsight(total.category(), toLongAmount(total.amount()));
    }

    private String buildInsightMessage(String question, BudgetInsightResponse insight) {
        String prompt = """
                너는 여행 정산 서비스 TogetherPay의 AI '루루'야.
                아래 예산/지출 집계 데이터만 기반으로 여행 예산 체크 인사이트를 제공해.
                
                규칙:
                - 제공되지 않은 지출, 예산, 환율, 장소 가격은 절대 지어내지 마.
                - 예산이 없으면 예산 설정이 필요하다고 안내해.
                - 숫자는 제공된 데이터만 사용해.
                - 3~5문장으로 모바일에서 읽기 좋게 답변해.
                - 한국어로 답변해.
                
                사용자 질문:
                %s
                
                예산/지출 데이터:
                tripId: %s
                totalBudget: %s
                spentAmount: %s
                remainingAmount: %s
                currency: %s
                usageRate: %.1f
                status: %s
                topCategories: %s
                """.formatted(
                question,
                insight.tripId(),
                insight.totalBudget(),
                insight.spentAmount(),
                insight.remainingAmount(),
                insight.currency(),
                insight.usageRate(),
                insight.status(),
                insight.topCategories()
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
            log.warn("OpenAI budget insight failed. tripId={}, message={}", insight.tripId(), e.getMessage());
        }

        return buildFallbackMessage(insight);
    }

    private String buildFallbackMessage(BudgetInsightResponse insight) {
        if ("NO_BUDGET".equals(insight.status())) {
            return "아직 이 여행의 예산이 설정되어 있지 않습니다. 현재 등록된 지출은 총 %,d입니다. 예산을 설정하면 남은 금액과 사용률 기준으로 더 정확하게 체크할 수 있습니다."
                    .formatted(insight.spentAmount());
        }

        return "현재까지 총 %,d %s 중 %,d %s를 사용했고, 남은 예산은 %,d %s입니다. 예산 사용률은 %.1f%%이며 상태는 %s입니다."
                .formatted(
                        insight.totalBudget(),
                        insight.currency(),
                        insight.spentAmount(),
                        insight.currency(),
                        insight.remainingAmount(),
                        insight.currency(),
                        insight.usageRate(),
                        insight.status()
                );
    }

    private Long toLongAmount(BigDecimal value) {
        return value == null ? 0L : value.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private double calculateUsageRate(Long totalBudget, Long spentAmount) {
        if (totalBudget == null || totalBudget <= 0) {
            return 0.0;
        }

        return BigDecimal.valueOf(spentAmount == null ? 0L : spentAmount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalBudget), 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private String resolveStatus(Long totalBudget, Long remainingAmount, double usageRate) {
        if (totalBudget == null) {
            return "NO_BUDGET";
        }
        if (remainingAmount != null && remainingAmount < 0) {
            return "OVER_BUDGET";
        }
        if (usageRate >= 80.0) {
            return "WARNING";
        }
        return "OK";
    }
}
