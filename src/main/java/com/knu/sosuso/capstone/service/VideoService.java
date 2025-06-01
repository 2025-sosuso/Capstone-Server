package com.knu.sosuso.capstone.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.sosuso.capstone.ai.dto.AnalysisResponse;
import com.knu.sosuso.capstone.config.ApiConfig;
import com.knu.sosuso.capstone.domain.Video;
import com.knu.sosuso.capstone.dto.response.VideoApiResponse;
import com.knu.sosuso.capstone.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
public class VideoService {

    private static final Logger logger = LoggerFactory.getLogger(VideoService.class);
    private static final String YOUTUBE_VIDEOS_API_URL = "https://www.googleapis.com/youtube/v3/videos";
    private static final String YOUTUBE_CHANNELS_API_URL = "https://www.googleapis.com/youtube/v3/channels";

    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile(
            "(?:youtube\\.com/(?:watch\\?v=|embed/|v/)|youtu\\.be/|m\\.youtube\\.com/watch\\?v=)([\\w-]{11})"
    );

    private final ApiConfig config = new ApiConfig();
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final VideoRepository videoRepository;

    /**
     * 비디오 id 추출
     * @param url
     * @return
     */
    public String extractVideoId(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }

        Matcher matcher = VIDEO_ID_PATTERN.matcher(url.trim());
        return matcher.find() ? matcher.group(1) : null;
    }

    public VideoApiResponse getVideoInfo(String videoId, Integer actualCommentCount) {
        if (videoId == null || videoId.trim().isEmpty()) {
            throw new IllegalArgumentException("비디오 ID는 필수입니다");
        }

        try {
            logger.info("비디오 정보 조회 시작: apiVideoId={}", videoId);

            // 1. 비디오 정보 조회
            String videoResponse = getVideoData(videoId.trim());
            JsonNode videoJson = objectMapper.readTree(videoResponse);

            if (!videoJson.has("items") || videoJson.get("items").isEmpty()) {
                throw new IllegalArgumentException("존재하지 않는 비디오입니다");
            }

            JsonNode videoItem = videoJson.get("items").get(0);
            String channelId = videoItem.get("snippet").get("channelId").asText();

            // 2. 채널 정보 조회
            String channelResponse = getChannelData(channelId);
            JsonNode channelJson = objectMapper.readTree(channelResponse);

            JsonNode channelItem = channelJson.get("items").get(0);

            // 3. 응답 생성
            VideoApiResponse response = buildVideoResponse(videoItem, channelItem, actualCommentCount);

            logger.info("비디오 정보 조회 완료: apiVideoId={}", videoId);
            return response;

        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("비디오를 찾을 수 없음: apiVideoId={}", videoId);
            throw new IllegalArgumentException("존재하지 않는 비디오입니다", e);

        } catch (HttpClientErrorException.Forbidden e) {
            logger.warn("비디오 접근 금지: apiVideoId={}", videoId);
            throw new IllegalStateException("이 비디오에 접근할 수 없습니다", e);

        } catch (RestClientException e) {
            logger.error("YouTube API 호출 실패: apiVideoId={}, error={}", videoId, e.getMessage(), e);
            throw new RuntimeException("비디오 정보를 가져올 수 없습니다", e);

        } catch (Exception e) {
            logger.error("비디오 정보 조회 실패: apiVideoId={}, error={}", videoId, e.getMessage(), e);
            throw new RuntimeException("비디오 정보 조회 중 오류 발생", e);
        }
    }

    /**
     * 비디오 데이터(분석 결과 포함) 저장 메서드
     * @param videoApiResponse
     * @param analysisResponse
     */
    public void saveVideoAnalysisInformation(VideoApiResponse videoApiResponse, AnalysisResponse analysisResponse) {
        Video video = Video.builder()
                .apiVideoId(videoApiResponse.apiVideoId())
                .title(videoApiResponse.title())
                .thumbnailUrl(videoApiResponse.thumbnailUrl())
                .channelId(videoApiResponse.channelId())
                .channelName((videoApiResponse.channelTitle()))
                .uploadedAt(videoApiResponse.publishedAt())
                .subscriberCount(videoApiResponse.subscriberCount())
                .viewCount(videoApiResponse.viewCount())
                .likeCount(videoApiResponse.likeCount())
                .commentCount(videoApiResponse.commentCount())
                .summation(analysisResponse.summation())
                .isWarning(analysisResponse.isWarning())
                .build();
        videoRepository.save(video);
    }

    /**
     * 유튜브에 해당 영상 데이터를 요청
     * @param videoId
     * @return
     */
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

    /**
     * 유튜브로부터 채널 데이터를 받아옴
     * @param channelId
     * @return
     */
    private String getChannelData(String channelId) {
        String apiUrl = UriComponentsBuilder.fromUriString(YOUTUBE_CHANNELS_API_URL)
                .queryParam("part", "snippet,statistics")
                .queryParam("id", channelId)
                .queryParam("key", config.getKey())
                .build(false)
                .toUriString();

        return restTemplate.getForObject(apiUrl, String.class);
    }

    /**
     * 유튜브로부터 영상 데이터를 받아옴
     * @param videoItem
     * @param channelItem
     * @param actualCommentCount
     * @return
     */
    private VideoApiResponse buildVideoResponse(JsonNode videoItem, JsonNode channelItem, Integer actualCommentCount) {
        JsonNode snippet = videoItem.get("snippet");
        JsonNode statistics = videoItem.get("statistics");
        JsonNode channelSnippet = channelItem.get("snippet");
        JsonNode channelStatistics = channelItem.get("statistics");

        // 썸네일 URL 추출 (standard 우선, 없으면 high, 없으면 medium, 없으면 default)
        String thumbnailUrl = extractThumbnailUrl(snippet.get("thumbnails"));
        String channelThumbnailUrl = extractThumbnailUrl(channelSnippet.get("thumbnails"));

        String commentCount;
        if (actualCommentCount != null) {
            commentCount = String.valueOf(actualCommentCount);
        } else {
            commentCount = statistics.has("commentCount") ? statistics.get("commentCount").asText() : "0";
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
                statistics.has("viewCount") ? statistics.get("viewCount").asText() : "0",
                statistics.has("likeCount") ? statistics.get("likeCount").asText() : "0",
                commentCount
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