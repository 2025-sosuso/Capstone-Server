package com.knu.sosuso.capstone.global.exception.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum TrendingError implements BaseError {

    UNSUPPORTED_CATEGORY(HttpStatus.BAD_REQUEST, "지원하지 않는 카테고리입니다."),
    INVALID_MAX_RESULTS(HttpStatus.BAD_REQUEST, "조회할 영상 개수는 1 이상 30 이하여야 합니다."),
    TRENDING_FETCH_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "인기급상승 목록을 가져올 수 없습니다."),
    TRENDING_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "인기급상승 영상 조회 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    TrendingError(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}