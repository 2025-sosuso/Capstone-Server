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

    // ========================================
    // 1. 메타데이터 갱신 (HTTP + 저장 통합)
    // ========================================

    /**
     * ✅ 메타데이터 갱신 (HTTP 호출 분리)
     * - HTTP 호출은 트랜잭션 밖
     * - DB 저장만 트랜잭션 안
     *
     * @param video 갱신할 Video 엔티티
     */
    public void refreshMetadata(Video video) {
        try {
            // 1. HTTP 호출 (트랜잭션 밖) - 커넥션 점유 X
            VideoApiResponse videoInfo = getVideoInfo(video.getApiVideoId());

            // 2. DB 저장 (트랜잭션 안) - 커넥션 0.01초만
            saveMetadataUpdate(video, videoInfo);

            log.info("✅ 메타데이터 갱신 완료: apiVideoId={}, updateCount={}",
                    video.getApiVideoId(), video.getMetadataUpdateCount() + 1);

        } catch (BusinessException e) {
            if (e.getError() == VideoError.VIDEO_NOT_FOUND) {
                // YouTube에서 삭제됨 → 삭제 플래그 설정
                log.warn("⚠️ YouTube에서 영상 삭제됨: apiVideoId={}", video.getApiVideoId());
                markAsDeleted(video);
            } else {
                throw e;
            }
        } catch (Exception e) {
            log.error("❌ 메타데이터 갱신 실패: apiVideoId={}, error={}",
                    video.getApiVideoId(), e.getMessage());
        }
    }

    /**
     * ✅ 메타데이터 갱신 (ID 기반 - 스케줄러용)
     */
    public void refreshMetadataById(Long videoId, String apiVideoId) {
        try {
            // 1. HTTP 호출 (트랜잭션 밖)
            VideoApiResponse videoInfo = getVideoInfo(apiVideoId);

            // 2. DB 저장 (트랜잭션 안)
            saveMetadataUpdateById(videoId, videoInfo);

            log.info("✅ 메타데이터 갱신 완료: videoId={}", videoId);

        } catch (BusinessException e) {
            if (e.getError() == VideoError.VIDEO_NOT_FOUND) {
                log.warn("⚠️ YouTube에서 영상 삭제됨: apiVideoId={}", apiVideoId);
                markAsDeletedById(videoId);
            } else {
                throw e;
            }
        } catch (Exception e) {
            log.error("❌ 메타데이터 갱신 실패: videoId={}, error={}", videoId, e.getMessage());
            throw new BusinessException(CommonError.VIDEO_PROCESSING_ERROR);
        }
    }

    /**
     * 메타데이터 저장 (트랜잭션)
     */
    @Transactional
    public void saveMetadataUpdate(Video video, VideoApiResponse videoInfo) {
        video.setViewCount(videoInfo.viewCount());
        video.setLikeCount(videoInfo.likeCount());
        video.setCommentCount(videoInfo.commentCount());
        video.setLastMetadataUpdatedAt(LocalDateTime.now());
        video.setMetadataUpdateCount(video.getMetadataUpdateCount() + 1);

        videoRepository.save(video);
    }

    /**
     * 메타데이터 저장 - ID 기반 (트랜잭션)
     */
    @Transactional
    public void saveMetadataUpdateById(Long videoId, VideoApiResponse videoInfo) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new BusinessException(VideoError.VIDEO_NOT_FOUND));

        video.setTitle(videoInfo.title());
        video.setDescription(videoInfo.description());
        video.setViewCount(videoInfo.viewCount());
        video.setLikeCount(videoInfo.likeCount());
        video.setCommentCount(videoInfo.commentCount());
        video.setThumbnailUrl(videoInfo.thumbnailUrl());
        video.setSubscriberCount(videoInfo.subscriberCount());
        video.setLastMetadataUpdatedAt(LocalDateTime.now());
        video.setMetadataUpdateCount(video.getMetadataUpdateCount() + 1);

        videoRepository.save(video);
    }

    // ========================================
    // 2. 삭제 확인 (HTTP + 저장 통합)
    // ========================================

    /**
     * ✅ 삭제 여부 확인 및 상태 업데이트 (HTTP 호출 분리)
     * - HTTP 호출은 트랜잭션 밖
     * - DB 저장만 트랜잭션 안
     *
     * @param video 확인할 Video 엔티티
     * @return true면 삭제됨
     */
    public boolean checkAndUpdateDeletionStatus(Video video) {
        // 1. HTTP 호출 (트랜잭션 밖) - 커넥션 점유 X
        boolean isDeleted = checkIfVideoDeleted(video.getApiVideoId());

        // 2. DB 저장 (트랜잭션 안) - 커넥션 0.01초만
        saveDeletionStatus(video, isDeleted);

        if (isDeleted) {
            log.info("🗑️ 영상 삭제 확인: apiVideoId={}", video.getApiVideoId());
        }

        return isDeleted;
    }

    /**
     * 삭제 상태 저장 (트랜잭션)
     */
    @Transactional
    public void saveDeletionStatus(Video video, boolean isDeleted) {
        if (isDeleted) {
            video.setDeleted(true);
        }
        video.setDeleteCheckedAt(LocalDateTime.now());
        videoRepository.save(video);
    }

    /**
     * 삭제 플래그 설정 (트랜잭션)
     */
    @Transactional
    public void markAsDeleted(Video video) {
        video.setDeleted(true);
        video.setDeleteCheckedAt(LocalDateTime.now());
        videoRepository.save(video);
    }

    /**
     * 삭제 플래그 설정 - ID 기반 (트랜잭션)
     */
    @Transactional
    public void markAsDeletedById(Long videoId) {
        try {
            Video video = videoRepository.findById(videoId).orElse(null);
            if (video != null) {
                video.setDeleted(true);
                video.setDeleteCheckedAt(LocalDateTime.now());
                videoRepository.save(video);
                log.info("🗑️ 영상 삭제 플래그 설정: videoId={}", videoId);
            }
        } catch (Exception e) {
            log.error("❌ 삭제 플래그 설정 실패: videoId={}, error={}", videoId, e.getMessage());
        }
    }

    // ========================================
    // 3. YouTube API 호출 (기존 유지)
    // ========================================

    /**
     * YouTube API로 비디오 정보 조회
     */
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
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("비디오 정보 조회 중 예상치 못한 오류: {}", e.getMessage());
            throw new BusinessException(CommonError.INTERNAL_SERVER_ERROR);
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

    // ========================================
    // 4. 비디오 저장 (기존 유지)
    // ========================================

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

    // ==================== Private 헬퍼 메서드 ====================

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

    private String getChannelData(String channelId) {
        String apiUrl = UriComponentsBuilder.fromUriString(YOUTUBE_CHANNELS_API_URL)
                .queryParam("part", "snippet,statistics")
                .queryParam("id", channelId)
                .queryParam("key", apiConfig.getKey())
                .build(false)
                .toUriString();

        return restTemplate.getForObject(apiUrl, String.class);
    }

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

    private String extractThumbnailByType(JsonNode thumbnails, VideoType videoType) {
        if (thumbnails == null) return "";

        if (videoType == VideoType.SHORTS) {
            if (thumbnails.has("medium")) {
                return thumbnails.get("medium").get("url").asText();
            }
            if (thumbnails.has("default")) {
                return thumbnails.get("default").get("url").asText();
            }
        } else {
            if (thumbnails.has("standard")) {
                return thumbnails.get("standard").get("url").asText();
            }
            if (thumbnails.has("high")) {
                return thumbnails.get("high").get("url").asText();
            }
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