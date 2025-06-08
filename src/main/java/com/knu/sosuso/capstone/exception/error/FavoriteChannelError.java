package com.knu.sosuso.capstone.exception.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum FavoriteChannelError implements BaseError {

    FAVORITE_CHANNEL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 등록된 관심 채널입니다."),
    FAVORITE_CHANNEL_NOT_FOUND(HttpStatus.NOT_FOUND, "등록되어있지 않은 관심 채널입니다."),
    FORBIDDEN_FAVORITE_CHANNEL_DELETE(HttpStatus.FORBIDDEN, "본인의 관심 채널만 취소할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    FavoriteChannelError(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
