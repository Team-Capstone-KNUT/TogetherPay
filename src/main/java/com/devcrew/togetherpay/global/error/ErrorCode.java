package com.devcrew.togetherpay.global.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ==========================================
    // 1. 공통 (Common)
    // ==========================================
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C001", "서버 내부 오류입니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C002", "잘못된 입력입니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C003", "입력 타입이 유효하지 않습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C004", "허용되지 않은 HTTP 메서드입니다."),

    // ==========================================
    // 2. 권한/인증 (Auth/Role) - 재사용성 높은 에러들 모음
    // ==========================================
    NOT_A_TEAM_LEADER(HttpStatus.FORBIDDEN, "R001", "방장만 접근 가능한 기능입니다."),
    NOT_A_TEAM_USER(HttpStatus.FORBIDDEN, "R002", "해당 팀에 소속된 멤버가 아닙니다."), // 여러 도메인에서 중복 사용되던 것을 하나로 통일
    NOT_MATCH_USER(HttpStatus.FORBIDDEN, "R003", "본인의 데이터에만 접근할 수 있습니다."), // S003 이동 및 범용적으로 수정
    NOT_A_TRIP_USER(HttpStatus.FORBIDDEN, "R004", "해당 여행에 등록된 사용자가 아닙니다."),

    // ==========================================
    // 3. 유저 (User)
    // ==========================================
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "존재하지 않는 회원입니다."),
    USER_ALREADY_WITHDRAWN(HttpStatus.BAD_REQUEST, "U002", "이미 탈퇴 처리 된 회원입니다."),

    // ==========================================
    // 4. 팀 (Team)
    // ==========================================
    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "T001", "존재하지 않는 팀입니다."),
    INVALID_TEAM_PASSWORD(HttpStatus.BAD_REQUEST, "T002", "잘못된 팀 비밀번호입니다."),
    ALREADY_TEAM_MEMBER(HttpStatus.CONFLICT, "T003", "이미 팀에 가입된 멤버입니다."),
    CANNOT_KICK_SELF(HttpStatus.BAD_REQUEST, "T004", "자기 자신을 강퇴할 수 없습니다."),
    TEAM_LEADER_CANNOT_LEAVE(HttpStatus.CONFLICT, "T005", "방장은 팀을 위임하거나 팀을 삭제하기 전까지 탈퇴할 수 없습니다."),
    TEAM_NAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "T006", "이미 동일한 이름으로 생성된 팀이 존재합니다."),
    NOT_A_TEAM_TRIP(HttpStatus.FORBIDDEN, "T007", "해당 팀에 등록된 여행이 아닙니다."),

    // ==========================================
    // 5. 여행 (Trip) - T 접두사 충돌 방지를 위해 TR 사용
    // ==========================================
    TRIP_NOT_FOUND(HttpStatus.NOT_FOUND, "TR001", "해당 여행이 존재하지 않습니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "TR002", "여행 시작일은 종료일보다 이전이어야 합니다."), // 신규 추가

    // ==========================================
    // 6. 예산 (Budget)
    // ==========================================
    BUDGET_ALREADY_EXISTS(HttpStatus.CONFLICT, "B001", "해당 날짜에는 이미 예산이 설정되어 있습니다."),
    BUDGET_NOT_FOUND(HttpStatus.NOT_FOUND, "B002", "등록된 예산을 찾을 수 없습니다."),
    BUDGET_CANNOT_BE_LESS_THAN_EXPENSE(HttpStatus.BAD_REQUEST,"B003", "이미 지출한 금액보다 적게 예산을 설정할 수 없습니다."), // 상태 코드 400으로 변경

    // ==========================================
    // 7. 지출 (Expense)
    // ==========================================
    EXPENSE_NOT_FOUND(HttpStatus.NOT_FOUND, "E001", "존재하지 않는 지출입니다."),
    INVALID_PAYER_COUNT(HttpStatus.BAD_REQUEST, "E002", "결제자는 반드시 1명이어야 합니다."),
    EXPENSE_DATE_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "E003", "지출 날짜가 해당 여행 기간 내에 존재하지 않습니다."),

    // ==========================================
    // 8. 정산 (Settlement)
    // ==========================================
    SETTLEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "존재하지 않는 정산입니다."),
    PAYER_NOT_FOUND(HttpStatus.NOT_FOUND, "S002", "결제자를 찾을 수 없습니다."),
    ALREADY_SETTLED(HttpStatus.CONFLICT, "S003", "이미 정산 요청이 완료된 지출 내역입니다."),
    ALREADY_TRANSFERRED(HttpStatus.CONFLICT, "S004", "이미 송금이 완료된 내역입니다."),
    // (S003 NOT_MATCH_USER 는 공통 권한 에러 R003 으로 통합되었습니다.)

    // ==========================================
    // 9. 일정 (schedule)
    SCHEDULE_ALREADY_EXISTS(HttpStatus.CONFLICT, "SC001", "해당 날짜에는 이미 일정이 생성되어 있습니다."),
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "SC002", "존재하지 않는 일정입니다."),
    SCHEDULE_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "SC003", "존재하지 않는 일정입니다."),
    INVALID_SCHEDULE_DATE(HttpStatus.BAD_REQUEST, "SC004", "해당 일정 날짜는 여행 기간에 포함되지 않습니다.");

    // ==========================================

    private final HttpStatus status;
    private final String code;
    private final String message;
}