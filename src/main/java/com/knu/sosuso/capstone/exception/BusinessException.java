package com.knu.sosuso.capstone.exception;

import com.knu.sosuso.capstone.exception.error.BaseError;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String message;

    public BusinessException(BaseError baseError) {
        super(baseError.getMessage());
        this.httpStatus = baseError.getHttpStatus();
        this.message = baseError.getMessage();
    }
}
