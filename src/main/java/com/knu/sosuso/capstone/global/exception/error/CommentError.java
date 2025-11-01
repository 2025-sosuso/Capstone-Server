package com.knu.sosuso.capstone.global.exception.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CommentError implements BaseError {

    COMMENT_FETCH_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "댓글 수집 중 오류가 발생했습니다."),
    COMMENT_SAVE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "댓글 저장 중 오류가 발생했습니다."),
    COMMENT_SEARCH_CONDITION_REQUIRED(HttpStatus.BAD_REQUEST, "검색 조건이 필요합니다."),
    INVALID_SENTIMENT_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 감정 타입입니다."),
    COMMENTS_DISABLED(HttpStatus.FORBIDDEN, "댓글이 비활성화된 영상입니다.");

    private final HttpStatus httpStatus;
    private final String message;

    CommentError(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}