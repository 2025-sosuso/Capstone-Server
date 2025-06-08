package com.knu.sosuso.capstone.exception.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum VideoError implements BaseError {

    VIDEO_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 영상입니다.");

    private final HttpStatus httpStatus;
    private final String message;

    VideoError(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
