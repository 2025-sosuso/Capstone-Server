package com.knu.sosuso.capstone.domain.comment.entity.value;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DetailSentimentType {
    // positive
    JOY, LOVE, GRATITUDE,
    // negative
    ANGER, SADNESS, FEAR,
    // other
    NEUTRAL;

    @JsonCreator
    public static DetailSentimentType fromString(String value) {
        if (value == null) {
            return NEUTRAL;
        }
        try {
            return DetailSentimentType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NEUTRAL;
        }
    }
}
