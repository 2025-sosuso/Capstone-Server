package com.knu.sosuso.capstone.global.exception.error;

import org.springframework.http.HttpStatus;

public interface BaseError {

    HttpStatus getHttpStatus();

    String getMessage();
}
