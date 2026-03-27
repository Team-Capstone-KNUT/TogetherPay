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
    USER_ALREADY_WITHDRAWN(HttpStatus.BAD_REQUEST, "U002", "이미 탈퇴 처리 된 회원입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
