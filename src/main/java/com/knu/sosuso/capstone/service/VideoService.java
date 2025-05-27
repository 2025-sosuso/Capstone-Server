package com.knu.sosuso.capstone.service;

import com.knu.sosuso.capstone.config.ApiConfig;
import com.knu.sosuso.capstone.dto.response.VideoApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VideoService {

    private static final Logger logger = LoggerFactory.getLogger(VideoService.class);
    private static final String YOUTUBE_VIDEOS_API_URL = "https://www.googleapis.com/youtube/v3/videos";

    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile(
            "(?:youtube\\.com/(?:watch\\?v=|embed/|v/)|youtu\\.be/|m\\.youtube\\.com/watch\\?v=)([\\w-]{11})"
    );

    private final ApiConfig config;
    private final RestTemplate restTemplate;

    public VideoService(ApiConfig config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
    }

    public String extractVideoId(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }

        Matcher matcher = VIDEO_ID_PATTERN.matcher(url.trim());
        return matcher.find() ? matcher.group(1) : null;
    }

    public VideoApiResponse getVideoInfo(String videoId) {
        if (videoId == null || videoId.trim().isEmpty()) {
            throw new IllegalArgumentException("비디오 ID는 필수입니다");
        }

        try {
            logger.info("비디오 정보 조회 시작: videoId={}", videoId);

            String apiUrl = UriComponentsBuilder.fromUriString(YOUTUBE_VIDEOS_API_URL)
                    .queryParam("part", "snippet,statistics")
                    .queryParam("id", videoId.trim())
                    .queryParam("key", config.getKey())
                    .queryParam("hl", "ko")
                    .build(false)
                    .toUriString();

            VideoApiResponse response = restTemplate.getForObject(apiUrl, VideoApiResponse.class);

            if (response.items() == null || response.items().length == 0) {
                throw new IllegalArgumentException("존재하지 않는 비디오입니다");
            }

            logger.info("비디오 정보 조회 완료: videoId={}", videoId);
            return response;

        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("비디오를 찾을 수 없음: videoId={}", videoId);
            throw new IllegalArgumentException("존재하지 않는 비디오입니다", e);

        } catch (HttpClientErrorException.Forbidden e) {
            logger.warn("비디오 접근 금지: videoId={}", videoId);
            throw new IllegalStateException("이 비디오에 접근할 수 없습니다", e);

        } catch (RestClientException e) {
            logger.error("YouTube API 호출 실패: videoId={}, error={}", videoId, e.getMessage(), e);
            throw new RuntimeException("비디오 정보를 가져올 수 없습니다", e);

        } catch (Exception e) {
            logger.error("비디오 정보 조회 실패: videoId={}, error={}", videoId, e.getMessage(), e);
            throw new RuntimeException("비디오 정보 조회 중 오류 발생", e);
        }
    }
}
