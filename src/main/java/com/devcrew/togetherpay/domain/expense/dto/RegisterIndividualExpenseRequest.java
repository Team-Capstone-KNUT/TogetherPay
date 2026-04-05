package com.devcrew.togetherpay.domain.expense.dto;

import com.devcrew.togetherpay.domain.expense.Category;
import com.devcrew.togetherpay.domain.expense.Currency;
import com.devcrew.togetherpay.domain.expense.PaymentMethod;
import com.devcrew.togetherpay.domain.expense.controller.command.RegisterIndividualExpenseCommand;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record RegisterIndividualExpenseRequest(
    @NotNull(message = "팀 ID는 필수입니다.")
    Long teamId,

    @NotBlank(message = "지출 제목을 입력해주세요.")
    @Size(min = 2, max = 50, message = "제목은 2자 이상 50자 이하로 입력해주세요.")
    String title,

    @Nullable
    String description,

    @NotNull(message = "통화 코드를 선택해주세요.")
    Currency currency,

    @NotNull(message = "카테고리를 선택해주세요.")
    Category category,

    @NotNull(message = "결제한 날짜를 입력해주세요.")
    LocalDate expenseDate,

    @NotNull(message = "결제 수단을 선택해주세요.")
    PaymentMethod method,

    @NotEmpty(message = "정산 대상자는 최소 1명 이상이어야 합니다.")
    List<ParticipantRequest> participantRequests

) {

  public RegisterIndividualExpenseCommand toCommand() {
    return RegisterIndividualExpenseCommand.builder()
        .teamId(teamId)
        .title(title)
        .description(description)
        .currency(currency)
        .category(category)
        .method(method)
        .participantInfos(from(participantRequests))
        .build();

  }

  // 검증된 값 ParticipantInfo로 감싸기.
  public static List<ParticipantInfo> from(List<ParticipantRequest> participantRequests) {
    return participantRequests.stream()
        .map(p -> {
          return ParticipantInfo.of(p.userId, p.isPayer, p.amount);
        }).toList();
  }

  // 내부 검증 레코드
  @Builder
  public record ParticipantRequest(
      @NotNull
      Long userId,
      @NotNull
      boolean isPayer, // 결제자 여부
      @NotNull
      BigDecimal amount

  ) {

  }


}
