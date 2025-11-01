package com.knu.sosuso.capstone.global.exception.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum VideoError implements BaseError {

    VIDEO_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 영상입니다."),
    VIDEO_DELETED(HttpStatus.GONE, "삭제된 영상입니다."),
    VIDEO_ID_REQUIRED(HttpStatus.BAD_REQUEST, "비디오 ID는 필수입니다."),
    INVALID_YOUTUBE_URL(HttpStatus.BAD_REQUEST, "유효하지 않은 YouTube URL입니다.");

    private final HttpStatus httpStatus;
    private final String message;

    VideoError(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}