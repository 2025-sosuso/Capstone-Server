package com.knu.sosuso.capstone.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.sosuso.capstone.config.ApiConfig;
import com.knu.sosuso.capstone.dto.response.CommentApiResponse;
import com.knu.sosuso.capstone.dto.response.SearchUrlResponse;
import com.knu.sosuso.capstone.dto.response.VideoApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TrendingService {
    private static final String YOUTUBE_VIDEOS_API_URL = "https://www.googleapis.com/youtube/v3/videos";

    private final VideoService videoService;
    private final CommentService commentService;
    private final ApiConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public List<SearchUrlResponse> getTrendingVideoWithComments(String categoryType, String regionCode, int maxResults) {
        log.info("인기급상승 영상 조회 시작: categoryType={}, regionCode={}, maxResults={}", categoryType, regionCode, maxResults);

        try {
            String categoryId = getCategoryId(categoryType);

            List<String> videoIds = fetchTrendingVideoIds(categoryId, regionCode, maxResults);

            if (videoIds.isEmpty()) {
                log.warn("인기급상승 영상이 없습니다: categoryType={}, regionCode={}", categoryType, regionCode);
                return new ArrayList<>();
            }

            List<SearchUrlResponse> results = new ArrayList<>();

            for (String videoId : videoIds) {
                try {
                    log.debug("비디오 정보 조회 중: videoId={}", videoId);

                    CommentApiResponse commentInfo = commentService.getCommentInfoAndSave(videoId);

                    VideoApiResponse videoInfo = videoService.getVideoInfo(videoId, commentInfo.allComments().size());

                    results.add(new SearchUrlResponse(videoInfo, commentInfo));
                } catch (Exception e) {
                    log.error("개별 비디오 처리 실패: videoId={}, error={}", videoId, e.getMessage(), e);
                }
            }

            log.info("인기급상승 영상 조회 완료: 요청={}, 성공={}", videoIds.size(), results.size());
            return results;
        } catch (Exception e) {
            log.error("인기급상승 영상 조회 실패: categoryType={}, error={}", categoryType, e.getMessage(), e);
            throw new RuntimeException("인기급상승 영상 조회 중 오류 발생", e);
        }
    }

    private String getCategoryId(String categoryType) {
        return switch (categoryType.toLowerCase()) {
            case "latest" -> "0";
            case "music" -> "10";
            case "game" -> "20";
            default -> throw new IllegalArgumentException("지원하지 않는 카테고리입니다: " + categoryType);
        };
    }

    private List<String> fetchTrendingVideoIds(String categoryId, String regionCode, int maxResults) {
        try {
            String apiUrl = buildTrendingApiUrl(categoryId, regionCode, maxResults);
            log.debug("인기급상승 API 호출: {}", apiUrl);

            String jsonResponse = restTemplate.getForObject(apiUrl, String.class);
            JsonNode rootNode = objectMapper.readTree(jsonResponse);

            List<String> videoIds = new ArrayList<>();
            JsonNode itemsNode = rootNode.path("items");

            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    String videoId = item.path("id").asText();
                    if (!videoId.isEmpty()) {
                        videoIds.add(videoId);
                    }
                }
            }

            log.info("인기급상승 비디오 ID 조회 완료: 개수={}", videoIds.size());
            return videoIds;
        } catch (Exception e) {
            log.error("인기급상승 비디오 ID 조회 실패: categoryId={}, error={}", categoryId, e.getMessage(), e);
            throw new RuntimeException("인기급상승 목록을 가져올 수 없습니다", e);
        }
    }

    private String buildTrendingApiUrl(String categoryId, String regionCode, int maxResults) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(YOUTUBE_VIDEOS_API_URL)
                .queryParam("part", "id")
                .queryParam("chart", "mostPopular")
                .queryParam("regionCode", regionCode)
                .queryParam("maxResults", Math.min(maxResults, 50))  // YouTube API 제한
                .queryParam("key", config.getKey());

        builder.queryParam("videoCategoryId", categoryId);

        return builder.build(false).toUriString();
    }

}
