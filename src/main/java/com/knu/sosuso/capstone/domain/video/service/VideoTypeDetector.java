package com.knu.sosuso.capstone.domain.video.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.knu.sosuso.capstone.domain.video.entity.VideoType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * 영상 타입 판별기
 * 1. Shorts URL 확인 (가장 확실)
 * 2. 썸네일 비율 확인 (세로 > 가로 = 쇼츠)
 */
@Slf4j
@Component
public class VideoTypeDetector {

    private final RestTemplate fastRestTemplate;

    public VideoTypeDetector(@Qualifier("fastRestTemplate") RestTemplate fastRestTemplate) {
        this.fastRestTemplate = fastRestTemplate;
    }

    /**
     * 영상 타입 판별
     */
    public VideoType detectVideoType(String apiVideoId, JsonNode thumbnailData) {
        if (isShortsUrlValid(apiVideoId)) {
            log.debug("Shorts URL 확인: apiVideoId={} → SHORTS", apiVideoId);
            return VideoType.SHORTS;
        }

        if (isShortsAspectRatio(thumbnailData)) {
            log.debug("썸네일 비율 확인: apiVideoId={} → SHORTS", apiVideoId);
            return VideoType.SHORTS;
        }

        log.debug("일반 영상 확인: apiVideoId={} → VIDEO", apiVideoId);
        return VideoType.VIDEO;
    }

    /**
     * Shorts URL 유효성 확인 (타임아웃: 연결 2초, 읽기 3초)
     */
    private boolean isShortsUrlValid(String apiVideoId) {
        try {
            String shortsUrl = "https://www.youtube.com/shorts/" + apiVideoId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = fastRestTemplate.exchange(
                    shortsUrl,
                    HttpMethod.HEAD,
                    entity,
                    String.class
            );

            return response.getStatusCode() == HttpStatus.OK;

        } catch (HttpClientErrorException e) {
            log.debug("Shorts URL 실패: apiVideoId={}, status={}", apiVideoId, e.getStatusCode());
            return false;

        } catch (Exception e) {
            log.debug("Shorts URL 확인 타임아웃: apiVideoId={}, error={}", apiVideoId, e.getMessage());
            return false;
        }
    }

    /**
     * 썸네일 비율로 쇼츠 판별 (세로 > 가로 = 쇼츠)
     */
    private boolean isShortsAspectRatio(JsonNode thumbnailData) {
        try {
            JsonNode high = thumbnailData.path("high");
            JsonNode medium = thumbnailData.path("medium");
            JsonNode defaultThumb = thumbnailData.path("default");

            JsonNode target = high.isMissingNode()
                    ? (medium.isMissingNode() ? defaultThumb : medium)
                    : high;

            if (target.isMissingNode()) {
                return false;
            }

            int width = target.path("width").asInt(0);
            int height = target.path("height").asInt(0);

            if (width == 0 || height == 0) {
                return false;
            }

            return height > width;

        } catch (Exception e) {
            log.warn("썸네일 비율 파싱 실패: {}", e.getMessage());
            return false;
        }
    }
}