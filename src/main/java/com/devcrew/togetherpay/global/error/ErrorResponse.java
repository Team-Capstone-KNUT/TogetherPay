package com.devcrew.togetherpay.global.error;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

@Getter
@Builder
public class ErrorResponse {
    @Builder.Default // timestamp null 이슈 방어
    private final LocalDateTime timestamp = LocalDateTime.now();
    private final int status;
    private final String error;
    private final String code;
    private final String message;

    // ErrorCode를 받아서 ResponseEntity 생성(단순 메시지 getter(.getMessage())로 가져오기만 함)
    public static ResponseEntity<ErrorResponse> toResponseEntity(ErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.builder()
                        .status(errorCode.getStatus().value())
                        .error(errorCode.getStatus().name())
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build()
                );
    };

    // 위와는 다르게 구체적인 메시지가 필요한 상황(예: 검증 에러(BindingError)에 사용할 메서드
    public static ResponseEntity<ErrorResponse> toResponseEntity(ErrorCode errorCode, String customMessage) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.builder()
                        .status(errorCode.getStatus().value())
                        .error(errorCode.getStatus().name())
                        .code(errorCode.getCode())
                        .message(customMessage)
                        .build()
                );
    }
}
