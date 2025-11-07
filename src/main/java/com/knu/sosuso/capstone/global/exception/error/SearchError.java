package com.knu.sosuso.capstone.global.exception.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum SearchError implements BaseError {

    SEARCH_QUERY_REQUIRED(HttpStatus.BAD_REQUEST, "검색어는 필수입니다."),
    SEARCH_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "검색 중 오류가 발생했습니다."),
    INVALID_PAGE_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 페이지 토큰입니다.");

    private final HttpStatus httpStatus;
    private final String message;

    SearchError(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}