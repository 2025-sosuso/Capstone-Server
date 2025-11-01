package com.knu.sosuso.capstone.global.exception.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AuthenticationError implements BaseError {

    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "토큰이 없습니다. 로그인이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    TOKEN_PARSING_ERROR(HttpStatus.UNAUTHORIZED, "토큰 파싱 중 오류가 발생했습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자입니다.");

    private final HttpStatus httpStatus;
    private final String message;

    AuthenticationError(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}