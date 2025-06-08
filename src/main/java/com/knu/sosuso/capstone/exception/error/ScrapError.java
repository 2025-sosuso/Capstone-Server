package com.knu.sosuso.capstone.exception.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ScrapError implements BaseError {

    SCRAP_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 스크랩된 영상입니다."),
    SCRAP_NOT_FOUNT(HttpStatus.NOT_FOUND, "존재하지 않는 스크랩입니다."),
    FORBIDDEN_SCRAP_DELETE(HttpStatus.FORBIDDEN, "본인의 스크랩만 취소할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    ScrapError(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
