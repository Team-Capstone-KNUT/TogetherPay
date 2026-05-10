package com.devcrew.togetherpay.domain.expense;

import com.devcrew.togetherpay.domain.expense.dto.ParticipantResponse;
import com.devcrew.togetherpay.domain.settlement.Settlement;
import com.devcrew.togetherpay.domain.trip.Trip;
import com.devcrew.togetherpay.global.common.BaseTimeEntity;
import com.devcrew.togetherpay.global.common.vo.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Expense extends BaseTimeEntity {

  @Column(name = "expense_id")
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String title;

  @Column(nullable = true)
  private String description;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private Currency currency; // 통화 코드

  @Column(nullable = false)
  private LocalDate expenseDate;

  @Column(nullable = false)
  private boolean isDutchPay; // 더치페이 여부 저장

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "amount",
          column = @Column(name = "exchange_rate",
              precision = 15, scale = 2,
              nullable = true))
  })
  private Money exchangeRate; // 선택한 날짜의 환율 스냅샷.

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "amount",
          column = @Column(name = "total_amount",
              precision = 15, scale = 2,
              nullable = false))
  })
  private Money totalAmount; // 전체 금액

  @Column(name = "krw_total_amount", nullable = true)
  private Integer krwTotalAmount; // 환율 * 외화 = 전체 금액(KRW)

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private Category category;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private PaymentMethod paymentMethod;

  // 연관관계 추가(Trip)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trip_id")
  private Trip trip;

  @Builder.Default
  @OneToMany(mappedBy = "expense", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Participant> participants = new ArrayList<>();

  @Builder.Default
  @OneToMany(mappedBy = "expense", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Settlement> settlements = new ArrayList<>();

  public BigDecimal getTotalAmount() {
    return this.totalAmount.getAmount();
  }

  public BigDecimal getExchangeRate() {
    if (this.exchangeRate == null) return null;
    return this.exchangeRate.getAmount();
  }

  public void addParticipant(Participant participant) {
    this.participants.add(participant);
  }

  public void clearParticipants() {
    this.participants.clear();
  }

  public void addSettlements(List<Settlement> settlements) {
    this.settlements.addAll(settlements);
  }

  // 더치 페이
  public void markAsDutchPay() {
    this.isDutchPay = true;
  }

  // 개인 결제
  public void markAsIndividualPay() {
    this.isDutchPay = false;
  }

  // 전체 금액 * 환율 = 전체 금액(KRW) 지정.
  public void calculateKRW() {
    if (this.exchangeRate == null) { // KRW인 경우 null이다.
      this.krwTotalAmount = this.totalAmount.toWons();
      return;
    }

    Money result = totalAmount.calculateMultiply(getExchangeRate());
    this.krwTotalAmount = result.toWons();
  }

  public List<ParticipantResponse> participantsToResponse() {
    return participants.stream()
        .map(p -> {
          return ParticipantResponse.builder()
              .participantId(p.getId())
              .userId(p.getUser().getId())
              .nickname(p.getUser().getNickname())
              .amount(p.getAmount())
              .isPayer(p.isPayer())
              .build();
        }).toList();
  }

  public void updateInfo(String title, String description, Currency currency, Category category, LocalDate expenseDate, PaymentMethod paymentMethod, BigDecimal totalAmount, BigDecimal exchangeRate) {
    this.title = title;
    this.description = description;
    this.currency = currency;
    this.category = category;
    this.expenseDate = expenseDate;
    this.paymentMethod = paymentMethod;
    this.totalAmount = Money.of(totalAmount);
    this.exchangeRate = Money.of(exchangeRate);
    calculateKRW();
  }

}
