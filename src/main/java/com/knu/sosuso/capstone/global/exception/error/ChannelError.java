package com.knu.sosuso.capstone.global.exception.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ChannelError implements BaseError {

    CHANNEL_QUERY_REQUIRED(HttpStatus.BAD_REQUEST, "검색어는 필수입니다."),
    CHANNEL_NOT_FOUND(HttpStatus.NOT_FOUND, "채널을 찾을 수 없습니다."),
    CHANNEL_API_ACCESS_DENIED(HttpStatus.FORBIDDEN, "YouTube API에 접근할 수 없습니다."),
    CHANNEL_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "채널 검색을 수행할 수 없습니다."),
    CHANNEL_LATEST_VIDEO_NOT_FOUND(HttpStatus.NOT_FOUND, "채널의 최근 영상을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    ChannelError(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}