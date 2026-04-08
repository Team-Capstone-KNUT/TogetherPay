package com.devcrew.togetherpay.global.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // 에러 코드 네이밍 부분은 도메인별로 접두사로 나누는 방법으로 진행.
    // 공통(Common)
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C001", "서버 내부 오류입니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C002", "잘못된 입력입니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C003", "입력 타입이 유효하지 않습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C004", "허용되지 않은 HTTP 메서드입니다."),

    // 유저(User)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "존재하지 않는 회원입니다."),
    USER_ALREADY_WITHDRAWN(HttpStatus.BAD_REQUEST, "U002", "이미 탈퇴 처리 된 회원입니다."),

    // 팀(Team)
    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "T001", "존재하지 않는 팀입니다."),
    INVALID_TEAM_PASSWORD(HttpStatus.BAD_REQUEST, "T002", "잘못된 팀 비밀번호입니다."),
    ALREADY_TEAM_MEMBER(HttpStatus.CONFLICT, "T003", "이미 팀에 가입된 멤버입니다."),
    CANNOT_KICK_SELF(HttpStatus.BAD_REQUEST, "T004", "자기 자신을 강퇴할 수 없습니다."),
    TEAM_LEADER_CANNOT_LEAVE(HttpStatus.CONFLICT, "T005", "방장은 팀을 위임하거나 팀을 삭제하기 전까지 탈퇴할 수 없습니다."),
    TEAM_NAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "T006", "이미 동일한 이름으로 생성된 팀이 존재합니다."),

    // 권한(Role)
    NOT_TEAM_LEADER(HttpStatus.FORBIDDEN, "R001", "방장만 접근 가능한 기능입니다."),
    TEAM_MEMBER_NOT_FOUND(HttpStatus.FORBIDDEN, "R002", "해당 팀에 소속된 멤버가 아닙니다."),

    // 예산(Budget)
    BUDGET_ALREADY_EXISTS(HttpStatus.CONFLICT, "B001", "해당 날짜에는 이미 예산이 설정되어 있습니다."),
    BUDGET_NOT_FOUND(HttpStatus.NOT_FOUND, "B002", "등록된 예산을 찾을 수 없습니다."),

    // 지출(Expense)
    EXPENSE_NOT_FOUND(HttpStatus.NOT_FOUND, "E001", "존재하지 않는 지출입니다."),
    NOT_A_TEAM_USER(HttpStatus.FORBIDDEN, "E002", "해당 팀에 속한 멤버가 아닙니다."),
    INVALID_PAYER_COUNT(HttpStatus.BAD_REQUEST, "E003", "결제자는 반드시 1명이어야 합니다."),

    // 정산(Settlement)
    SETTLEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "존재하지 않는 정산입니다."),
    PAYER_NOT_FOUND(HttpStatus.NOT_FOUND, "S002", "결제자를 찾을 수 없습니다."),
    NOT_MATCH_USER(HttpStatus.FORBIDDEN, "S003", "정산 요청 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
