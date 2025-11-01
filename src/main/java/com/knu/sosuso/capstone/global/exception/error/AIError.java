package com.knu.sosuso.capstone.global.exception.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AIError implements BaseError {

    AI_ANALYSIS_REQUEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI 분석 요청에 실패했습니다."),
    AI_ANALYSIS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AI 분석 중 오류가 발생했습니다."),
    FASTAPI_CONNECTION_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버와 연결할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    AIError(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}