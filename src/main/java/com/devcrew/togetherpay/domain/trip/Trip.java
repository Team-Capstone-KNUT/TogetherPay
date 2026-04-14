package com.devcrew.togetherpay.domain.trip;

import com.devcrew.togetherpay.domain.budget.Budget;
import com.devcrew.togetherpay.domain.expense.Currency;
import com.devcrew.togetherpay.domain.expense.Expense;
import com.devcrew.togetherpay.domain.team.Team;
import com.devcrew.togetherpay.global.common.BaseTimeEntity;
import com.devcrew.togetherpay.global.error.ErrorCode;
import com.devcrew.togetherpay.global.error.exception.BusinessException;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trip extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trip_id")
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private Currency baseCurrency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Builder.Default
    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Budget> budgets = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Expense> expenses = new ArrayList<>();

    // 여행 정보 수정 메서드
    public void updateInfo(String title, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void validateDate(LocalDate date) {
        if (date.isBefore(startDate) || date.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.EXPENSE_DATE_OUT_OF_RANGE);
        }
    }

    // 날짜 검증 메서드
    public void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }
    }
}
