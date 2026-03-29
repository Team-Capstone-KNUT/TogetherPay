package com.devcrew.togetherpay.domain.expense;

import com.devcrew.togetherpay.domain.expense.dto.ParticipantResponse;
import com.devcrew.togetherpay.global.common.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal totalAmount;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private Category category;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private PaymentMethod paymentMethod;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_id")
  private Team team;

  @OneToMany(mappedBy = "expense", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Participant> participants = new ArrayList<>();

  public void addParticipant(Participant participant) {
    this.participants.add(participant);
  }

  public List<ParticipantResponse> participantsToResponse() {
    return participants.stream()
        .map(p -> {
          return ParticipantResponse.builder()
              .participantId(p.getId())
              .userId(p.getUser().getId())
              .nickname(p.getUser().getName())
              .amount(p.getAmount())
              .isPayer(p.isPayer())
              .build();
        }).toList();
  }

  public void updateInfo(String title, String description, Currency currency, Category category, PaymentMethod paymentMethod, BigDecimal totalAmount) {
    this.title = title;
    this.description = description;
    this.currency = currency;
    this.category = category;
    this.paymentMethod = paymentMethod;
    this.totalAmount = totalAmount;
  }

  public void clearParticipants() {
    this.participants.clear();
  }
}
