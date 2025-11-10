package com.knu.sosuso.capstone.global.exception.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum SearchError implements BaseError {

    SEARCH_QUERY_REQUIRED(HttpStatus.BAD_REQUEST, "검색어는 필수입니다."),
    SEARCH_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "검색 중 오류가 발생했습니다."),
    INVALID_PAGE_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 페이지 토큰입니다."),
    YOUTUBE_API_QUOTA_EXCEEDED(HttpStatus.SERVICE_UNAVAILABLE, "일일 검색 한도를 초과했습니다. 내일 다시 이용해주세요."),
    YOUTUBE_API_ACCESS_DENIED(HttpStatus.FORBIDDEN, "YouTube API 접근이 거부되었습니다."),
    YOUTUBE_API_ERROR(HttpStatus.BAD_GATEWAY, "YouTube 서비스에 일시적인 문제가 있습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    SearchError(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}