package com.knu.sosuso.capstone.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final String YOUTUBE_CHANNELS_API_URL = "https://www.googleapis.com/youtube/v3/channels";

    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile(
            "(?:youtube\\.com/(?:watch\\?v=|embed/|v/)|youtu\\.be/|m\\.youtube\\.com/watch\\?v=)([\\w-]{11})"
    );

    private final ApiConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public VideoService(ApiConfig config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
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

            // 1. 비디오 정보 조회
            String videoResponse = getVideoData(videoId.trim());
            JsonNode videoJson = objectMapper.readTree(videoResponse);

            if (!videoJson.has("items") || videoJson.get("items").size() == 0) {
                throw new IllegalArgumentException("존재하지 않는 비디오입니다");
            }

            JsonNode videoItem = videoJson.get("items").get(0);
            String channelId = videoItem.get("snippet").get("channelId").asText();

            // 2. 채널 정보 조회
            String channelResponse = getChannelData(channelId);
            JsonNode channelJson = objectMapper.readTree(channelResponse);

            JsonNode channelItem = channelJson.get("items").get(0);

            // 3. 응답 생성
            VideoApiResponse response = buildVideoResponse(videoItem, channelItem);

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

    private String getVideoData(String videoId) {
        String apiUrl = UriComponentsBuilder.fromUriString(YOUTUBE_VIDEOS_API_URL)
                .queryParam("part", "snippet,statistics")
                .queryParam("id", videoId)
                .queryParam("key", config.getKey())
                .queryParam("hl", "ko")
                .build(false)
                .toUriString();

        return restTemplate.getForObject(apiUrl, String.class);
    }

    private String getChannelData(String channelId) {
        String apiUrl = UriComponentsBuilder.fromUriString(YOUTUBE_CHANNELS_API_URL)
                .queryParam("part", "snippet,statistics")
                .queryParam("id", channelId)
                .queryParam("key", config.getKey())
                .build(false)
                .toUriString();

        return restTemplate.getForObject(apiUrl, String.class);
    }

    private VideoApiResponse buildVideoResponse(JsonNode videoItem, JsonNode channelItem) {
        JsonNode snippet = videoItem.get("snippet");
        JsonNode statistics = videoItem.get("statistics");
        JsonNode channelSnippet = channelItem.get("snippet");
        JsonNode channelStatistics = channelItem.get("statistics");

        // 썸네일 URL 추출 (standard 우선, 없으면 high, 없으면 medium, 없으면 default)
        String thumbnailUrl = extractThumbnailUrl(snippet.get("thumbnails"));
        String channelThumbnailUrl = extractThumbnailUrl(channelSnippet.get("thumbnails"));

        // tags 배열 처리
        String[] tags = null;
        if (snippet.has("tags") && snippet.get("tags").isArray()) {
            tags = objectMapper.convertValue(snippet.get("tags"), String[].class);
        }

        return new VideoApiResponse(
                videoItem.get("id").asText(),
                snippet.get("publishedAt").asText(),
                snippet.get("title").asText(),
                snippet.has("description") ? snippet.get("description").asText() : "",
                snippet.get("channelId").asText(),
                snippet.get("channelTitle").asText(),
                channelThumbnailUrl,
                channelStatistics.has("subscriberCount") ? channelStatistics.get("subscriberCount").asText() : "0",
                thumbnailUrl,
                tags != null ? tags : new String[0],
                snippet.has("categoryId") ? snippet.get("categoryId").asText() : "",
                snippet.has("liveBroadcastContent") ? snippet.get("liveBroadcastContent").asText() : "",
                snippet.has("defaultAudioLanguage") ? snippet.get("defaultAudioLanguage").asText() : "",
                statistics.has("viewCount") ? statistics.get("viewCount").asText() : "0",
                statistics.has("likeCount") ? statistics.get("likeCount").asText() : "0",
                statistics.has("commentCount") ? statistics.get("commentCount").asText() : "0"
        );
    }

    private String extractThumbnailUrl(JsonNode thumbnails) {
        if (thumbnails == null) return "";

        // standard -> high -> medium -> default 순서로 우선순위
        if (thumbnails.has("standard")) {
            return thumbnails.get("standard").get("url").asText();
        }
        if (thumbnails.has("high")) {
            return thumbnails.get("high").get("url").asText();
        }
        if (thumbnails.has("medium")) {
            return thumbnails.get("medium").get("url").asText();
        }
        if (thumbnails.has("default")) {
            return thumbnails.get("default").get("url").asText();
        }
        return "";
    }
}