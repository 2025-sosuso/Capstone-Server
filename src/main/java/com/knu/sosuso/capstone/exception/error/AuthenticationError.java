package com.knu.sosuso.capstone.exception.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AuthenticationError implements BaseError {

    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 토큰입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다.");

    private final HttpStatus httpStatus;
    private final String message;

    AuthenticationError(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
