package com.knu.sosuso.capstone.domain.video.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.knu.sosuso.capstone.domain.video.entity.VideoType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * 영상 타입 정확한 판별기
 * 1. Shorts URL 확인 (가장 확실)
 * 2. 썸네일 비율 확인 (세로 > 가로 = 쇼츠)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoTypeDetector {

    private final RestTemplate restTemplate;

    /**
     * 영상 타입 판별
     * @param apiVideoId YouTube 비디오 ID
     * @param thumbnailData 썸네일 정보 (JSON)
     * @return VideoType.SHORTS 또는 VideoType.VIDEO
     */
    public VideoType detectVideoType(String apiVideoId, JsonNode thumbnailData) {
        // 1차: Shorts URL 확인 (가장 확실!)
        if (isShortsUrlValid(apiVideoId)) {
            log.info("Shorts URL 확인: apiVideoId={} → SHORTS", apiVideoId);
            return VideoType.SHORTS;
        }

        // 2차: 썸네일 비율 확인
        if (isShortsAspectRatio(thumbnailData)) {
            log.info("썸네일 비율 확인: apiVideoId={} → SHORTS", apiVideoId);
            return VideoType.SHORTS;
        }

        log.info("일반 영상 확인: apiVideoId={} → VIDEO", apiVideoId);
        return VideoType.VIDEO;
    }

    /**
     * Shorts URL 유효성 확인
     * https://www.youtube.com/shorts/{videoId} → 200 OK면 쇼츠
     */
    private boolean isShortsUrlValid(String apiVideoId) {
        try {
            String shortsUrl = "https://www.youtube.com/shorts/" + apiVideoId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    shortsUrl,
                    HttpMethod.HEAD,
                    entity,
                    String.class
            );

            // 200 OK → 쇼츠 확정
            return response.getStatusCode() == HttpStatus.OK;

        } catch (HttpClientErrorException e) {
            // 404, 301 등 → 일반 영상
            log.debug("Shorts URL 실패: apiVideoId={}, status={}", apiVideoId, e.getStatusCode());
            return false;

        } catch (Exception e) {
            log.warn("Shorts URL 확인 중 예외: apiVideoId={}, error={}", apiVideoId, e.getMessage());
            return false;
        }
    }

    /**
     * 썸네일 비율로 쇼츠 판별
     * 세로 비율 (height > width) → 쇼츠
     */
    private boolean isShortsAspectRatio(JsonNode thumbnailData) {
        try {
            // high, medium, default 순으로 확인
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

            // 세로 비율 = 쇼츠
            boolean isVertical = height > width;

            log.debug("썸네일 비율: {}x{} → {}", width, height, isVertical ? "세로(쇼츠)" : "가로(일반)");

            return isVertical;

        } catch (Exception e) {
            log.warn("썸네일 비율 파싱 실패: {}", e.getMessage());
            return false;
        }
    }
}