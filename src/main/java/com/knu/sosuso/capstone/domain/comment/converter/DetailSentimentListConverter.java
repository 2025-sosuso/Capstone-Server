package com.knu.sosuso.capstone.domain.comment.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.sosuso.capstone.domain.comment.entity.value.DetailSentimentType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DetailSentimentType List ↔ JSON String 변환
 * DB: ["JOY", "GRATITUDE"] (JSON String)
 * Entity: List<DetailSentimentType>
 */
@Slf4j
@Converter
public class DetailSentimentListConverter implements AttributeConverter<List<DetailSentimentType>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<DetailSentimentType> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }

        try {
            List<String> names = attribute.stream()
                    .map(Enum::name)
                    .collect(Collectors.toList());
            return objectMapper.writeValueAsString(names);
        } catch (JsonProcessingException e) {
            log.error("DetailSentimentType List → JSON 변환 실패", e);
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DetailSentimentType> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            List<String> names = objectMapper.readValue(dbData, List.class);
            return names.stream()
                    .map(name -> {
                        try {
                            return DetailSentimentType.valueOf(name);
                        } catch (IllegalArgumentException e) {
                            log.warn("Unknown DetailSentimentType: {}", name);
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (JsonProcessingException e) {
            log.error("JSON → DetailSentimentType List 변환 실패: {}", dbData, e);
            return new ArrayList<>();
        }
    }
}