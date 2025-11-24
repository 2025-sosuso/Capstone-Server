package com.knu.sosuso.capstone.domain.comment.entity.value;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum SentimentType {
    POSITIVE,
    NEGATIVE,
    OTHER;

    @JsonCreator
    public static SentimentType fromString(String value) {
        if (value == null) {
            return OTHER;
        }
        try {
            return SentimentType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }
}
