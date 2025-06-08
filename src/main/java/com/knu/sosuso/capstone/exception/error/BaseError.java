package com.knu.sosuso.capstone.exception.error;

import org.springframework.http.HttpStatus;

public interface BaseError {

    HttpStatus getHttpStatus();

    String getMessage();
}
