package com.knu.sosuso.capstone.global.exception.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CommonError implements BaseError {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    DATA_PARSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "데이터 파싱 중 오류가 발생했습니다."),
    DATA_CONVERSION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "데이터 변환 중 오류가 발생했습니다."),
    VIDEO_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "영상 처리 중 오류가 발생했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    CommonError(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}