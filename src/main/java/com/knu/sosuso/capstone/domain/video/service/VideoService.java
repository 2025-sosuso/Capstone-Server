package com.knu.sosuso.capstone.domain.video.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.sosuso.capstone.domain.video.dto.response.VideoApiResponse;
import com.knu.sosuso.capstone.domain.video.entity.Video;
import com.knu.sosuso.capstone.domain.video.entity.AIAnalysisStatus;
import com.knu.sosuso.capstone.domain.video.entity.VideoType;
import com.knu.sosuso.capstone.domain.video.repository.VideoRepository;
import com.knu.sosuso.capstone.global.config.ApiConfig;
import com.knu.sosuso.capstone.domain.comment.dto.response.CommentApiResponse;
import com.knu.sosuso.capstone.global.exception.BusinessException;
import com.knu.sosuso.capstone.global.exception.error.CommonError;
import com.knu.sosuso.capstone.global.exception.error.VideoError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
@Service
public class VideoService {

    private static final String YOUTUBE_VIDEOS_API_URL = "https://www.googleapis.com/youtube/v3/videos";
    private static final String YOUTUBE_CHANNELS_API_URL = "https://www.googleapis.com/youtube/v3/channels";

    private final ApiConfig apiConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final VideoRepository videoRepository;

    /**
     * YouTube API로 비디오 정보 조회
     */
    @Transactional(readOnly = true)
    public VideoApiResponse getVideoInfo(String apiVideoId) {
        try {
            log.info("YouTube API 호출 - 비디오 정보 조회: apiVideoId={}", apiVideoId);

            // 1. 비디오 데이터 조회
            String videoData = getVideoData(apiVideoId);
            JsonNode videoRootNode = objectMapper.readTree(videoData);

            // 2. 비디오 존재 여부 확인
            JsonNode videoItems = videoRootNode.path("items");
            if (videoItems.isEmpty()) {
                log.error("비디오를 찾을 수 없음: apiVideoId={}", apiVideoId);
                throw new BusinessException(VideoError.VIDEO_NOT_FOUND);
            }

            JsonNode videoItem = videoItems.get(0);
            String channelId = videoItem.path("snippet").path("channelId").asText();

            // 3. 채널 데이터 조회
            String channelData = getChannelData(channelId);
            JsonNode channelRootNode = objectMapper.readTree(channelData);
            JsonNode channelItem = channelRootNode.path("items").get(0);

            // 4. VideoApiResponse 생성
            return buildVideoResponse(videoItem, channelItem);

        } catch (HttpClientErrorException e) {
            log.error("YouTube API 에러: status={}, message={}", e.getStatusCode(), e.getMessage());
            if (e.getStatusCode().is4xxClientError()) {
                throw new BusinessException(VideoError.VIDEO_NOT_FOUND);
            }
            throw new BusinessException(CommonError.YOUTUBE_API_ERROR);
        } catch (RestClientException e) {
            log.error("YouTube API 네트워크 에러: {}", e.getMessage());
            throw new BusinessException(CommonError.YOUTUBE_API_ERROR);
        } catch (Exception e) {
            log.error("비디오 정보 조회 중 예상치 못한 오류: {}", e.getMessage());
            throw new BusinessException(CommonError.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 메타데이터만 저장 (댓글 없는 경우)
     */
    @Transactional
    public Video saveVideoMetadataOnly(VideoApiResponse videoApiResponse, VideoType videoType) {
        try {
            log.info("💾 비디오 메타데이터 저장: apiVideoId={}, type={}",
                    videoApiResponse.apiVideoId(), videoType);

            String thumbnailUrl = extractThumbnailByType(
                    videoApiResponse.thumbnails(),
                    videoType
            );

            Video video = Video.builder()
                    .apiVideoId(videoApiResponse.apiVideoId())
                    .title(videoApiResponse.title())
                    .description(videoApiResponse.description())
                    .viewCount(videoApiResponse.viewCount())
                    .likeCount(videoApiResponse.likeCount())
                    .commentCount(videoApiResponse.commentCount())
                    .thumbnailUrl(thumbnailUrl)
                    .videoType(videoType)
                    .channelId(videoApiResponse.channelId())
                    .channelName(videoApiResponse.channelTitle())
                    .channelThumbnailUrl(videoApiResponse.channelThumbnailUrl())
                    .subscriberCount(videoApiResponse.subscriberCount())
                    .uploadedAt(videoApiResponse.publishedAt())
                    .commentHistogram("{}")
                    .popularTimestamps("{}")
                    .commentsDisabled(false)
                    .hasNoComments(false)
                    .aiAnalysisStatus(AIAnalysisStatus.PENDING)
                    .lastMetadataUpdatedAt(LocalDateTime.now())
                    .build();

            return videoRepository.save(video);

        } catch (Exception e) {
            log.error("❌ 비디오 저장 실패: {}", e.getMessage());
            throw new BusinessException(VideoError.VIDEO_PROCESSING_ERROR);
        }
    }

    /**
     * 메타데이터 + 백엔드 분석 저장
     */
    @Transactional
    public Video saveVideoWithAnalysis(VideoApiResponse videoApiResponse,
                                       CommentApiResponse commentResponse,
                                       VideoType videoType) {
        try {
            log.info("💾 비디오 + 분석 저장: apiVideoId={}, type={}",
                    videoApiResponse.apiVideoId(), videoType);

            String thumbnailUrl = extractThumbnailByType(
                    videoApiResponse.thumbnails(),
                    videoType
            );

            Video video = Video.builder()
                    .apiVideoId(videoApiResponse.apiVideoId())
                    .title(videoApiResponse.title())
                    .description(videoApiResponse.description())
                    .viewCount(videoApiResponse.viewCount())
                    .likeCount(videoApiResponse.likeCount())
                    .commentCount(videoApiResponse.commentCount())
                    .thumbnailUrl(thumbnailUrl)
                    .videoType(videoType)
                    .channelId(videoApiResponse.channelId())
                    .channelName(videoApiResponse.channelTitle())
                    .channelThumbnailUrl(videoApiResponse.channelThumbnailUrl())
                    .subscriberCount(videoApiResponse.subscriberCount())
                    .uploadedAt(videoApiResponse.publishedAt())
                    .commentHistogram(objectMapper.writeValueAsString(commentResponse.commentHistogram()))
                    .popularTimestamps(objectMapper.writeValueAsString(commentResponse.popularTimestamps()))
                    .commentsDisabled(false)
                    .hasNoComments(false)
                    .aiAnalysisStatus(AIAnalysisStatus.PENDING)
                    .lastMetadataUpdatedAt(LocalDateTime.now())
                    .build();

            return videoRepository.save(video);

        } catch (Exception e) {
            log.error("❌ 비디오 저장 실패: {}", e.getMessage());
            throw new BusinessException(VideoError.VIDEO_PROCESSING_ERROR);
        }
    }

    /**
     * YouTube API로 영상 삭제 여부 확인
     */
    public boolean checkIfVideoDeleted(String apiVideoId) {
        try {
            String apiUrl = UriComponentsBuilder
                    .fromUriString(YOUTUBE_VIDEOS_API_URL)
                    .queryParam("part", "id")
                    .queryParam("id", apiVideoId)
                    .queryParam("key", apiConfig.getKey())
                    .build(false)
                    .toUriString();

            String jsonResponse = restTemplate.getForObject(apiUrl, String.class);
            JsonNode rootNode = objectMapper.readTree(jsonResponse);

            JsonNode items = rootNode.path("items");
            boolean isDeleted = items.isEmpty();

            if (isDeleted) {
                log.info("영상 삭제 확인됨: apiVideoId={}", apiVideoId);
            }

            return isDeleted;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().is4xxClientError()) {
                log.error("YouTube API 에러: {}", e.getMessage());
                return false;
            }
            throw e;
        } catch (Exception e) {
            log.error("영상 삭제 여부 확인 실패: apiVideoId={}, error={}", apiVideoId, e.getMessage());
            return false;
        }
    }

    // ==================== Private 헬퍼 메서드 ====================

    /**
     * 유튜브에 해당 영상 데이터를 요청
     *
     * @param videoId YouTube 비디오 ID
     * @return YouTube API로부터 받은 JSON 응답
     */
    private String getVideoData(String videoId) {
        String apiUrl = UriComponentsBuilder.fromUriString(YOUTUBE_VIDEOS_API_URL)
                .queryParam("part", "snippet,statistics")
                .queryParam("id", videoId)
                .queryParam("key", apiConfig.getKey())
                .queryParam("hl", "ko")
                .build(false)
                .toUriString();

        return restTemplate.getForObject(apiUrl, String.class);
    }

    /**
     * 유튜브로부터 채널 데이터를 받아옴
     *
     * @param channelId YouTube 채널 ID
     * @return YouTube API로부터 받은 채널 JSON 응답
     */
    private String getChannelData(String channelId) {
        String apiUrl = UriComponentsBuilder.fromUriString(YOUTUBE_CHANNELS_API_URL)
                .queryParam("part", "snippet,statistics")
                .queryParam("id", channelId)
                .queryParam("key", apiConfig.getKey())
                .build(false)
                .toUriString();

        return restTemplate.getForObject(apiUrl, String.class);
    }

    /**
     * 유튜브로부터 받은 데이터를 VideoApiResponse로 변환
     *
     * @param videoItem          YouTube API의 비디오 아이템 JSON
     * @param channelItem        YouTube API의 채널 아이템 JSON
     * @return 변환된 VideoApiResponse 객체
     */
    private VideoApiResponse buildVideoResponse(JsonNode videoItem, JsonNode channelItem) {
        JsonNode snippet = videoItem.get("snippet");
        JsonNode statistics = videoItem.get("statistics");
        JsonNode channelSnippet = channelItem.get("snippet");
        JsonNode channelStatistics = channelItem.get("statistics");

        String thumbnailUrl = extractThumbnailUrl(snippet.get("thumbnails"));
        String channelThumbnailUrl = extractThumbnailUrl(channelSnippet.get("thumbnails"));

        return new VideoApiResponse(
                videoItem.get("id").asText(),
                snippet.get("title").asText(),
                snippet.has("description") ? snippet.get("description").asText() : "",
                statistics.has("viewCount") ? statistics.get("viewCount").asText() : "0",
                statistics.has("likeCount") ? statistics.get("likeCount").asText() : "0",
                statistics.has("commentCount") ? statistics.get("commentCount").asText() : "0",
                thumbnailUrl,
                snippet.get("thumbnails"),
                snippet.get("channelId").asText(),
                snippet.get("channelTitle").asText(),
                channelThumbnailUrl,
                channelStatistics.has("subscriberCount") ? channelStatistics.get("subscriberCount").asText() : "0",
                snippet.get("publishedAt").asText()
        );
    }

    private String extractThumbnailUrl(JsonNode thumbnails) {
        if (thumbnails == null) return "";

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

    /**
     * 타입별 썸네일 선택
     */
    private String extractThumbnailByType(JsonNode thumbnails, VideoType videoType) {
        if (thumbnails == null) return "";

        if (videoType == VideoType.SHORTS) {
            // 쇼츠: 작은 크기 우선 (세로 비율)
            if (thumbnails.has("medium")) {
                return thumbnails.get("medium").get("url").asText();
            }
            if (thumbnails.has("default")) {
                return thumbnails.get("default").get("url").asText();
            }
        } else {
            // 일반 영상: 큰 크기 우선 (가로 비율)
            if (thumbnails.has("standard")) {
                return thumbnails.get("standard").get("url").asText();
            }
            if (thumbnails.has("high")) {
                return thumbnails.get("high").get("url").asText();
            }
        }

        // 기본값
        if (thumbnails.has("medium")) {
            return thumbnails.get("medium").get("url").asText();
        }
        if (thumbnails.has("default")) {
            return thumbnails.get("default").get("url").asText();
        }

        return "";
    }
}