package com.devcrew.togetherpay.global.error;

import com.devcrew.togetherpay.global.error.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.error("BusinessException: code={} mesasge={}", e.getErrorCode().getCode(), e.getMessage());
        return ErrorResponse.toResponseEntity(e.getErrorCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        // stream을 사용하니까 코드가 완전 깔끔해짐.
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("[%s]은(는) %s.", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining(" "));

        log.error("Validation Fail: {}", message);

        return ErrorResponse.toResponseEntity(ErrorCode.INVALID_INPUT_VALUE, message);
    }

    @ExceptionHandler
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Internal Server Error", e);
        return ErrorResponse.toResponseEntity(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
