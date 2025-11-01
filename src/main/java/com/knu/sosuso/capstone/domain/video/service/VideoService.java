package com.knu.sosuso.capstone.domain.video.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.sosuso.capstone.domain.ai.dto.AIAnalysisResponse;
import com.knu.sosuso.capstone.domain.conmment.service.CommentService;
import com.knu.sosuso.capstone.domain.video.dto.response.VideoApiResponse;
import com.knu.sosuso.capstone.domain.video.entity.Video;
import com.knu.sosuso.capstone.domain.video.repository.VideoRepository;
import com.knu.sosuso.capstone.global.config.ApiConfig;
import com.knu.sosuso.capstone.domain.conmment.dto.response.CommentApiResponse;
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

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
@Service
public class VideoService {

    private static final String YOUTUBE_VIDEOS_API_URL = "https://www.googleapis.com/youtube/v3/videos";
    private static final String YOUTUBE_CHANNELS_API_URL = "https://www.googleapis.com/youtube/v3/channels";

    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile(
            "(?:youtube\\.com/(?:watch\\?v=|embed/|v/)|youtu\\.be/|m\\.youtube\\.com/watch\\?v=)([\\w-]{11})"
    );

    private final ApiConfig apiConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final VideoRepository videoRepository;
    private final CommentService commentService;

    /**
     * 비디오 ID 추출
     *
     * @param url YouTube URL
     * @return 추출된 비디오 ID 또는 null
     */
    public String extractVideoId(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }

        Matcher matcher = VIDEO_ID_PATTERN.matcher(url.trim());
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * YouTube API로 비디오 정보 조회
     *
     * @param videoId            비디오 ID
     * @return 비디오 API 응답 객체
     */
    public VideoApiResponse getVideoInfo(String videoId) {
        if (videoId == null || videoId.trim().isEmpty()) {
            throw new BusinessException(VideoError.VIDEO_ID_REQUIRED);
        }

        try {
            log.info("비디오 정보 조회 시작: apiVideoId={}", videoId);

            // 1. 비디오 정보 조회
            String videoResponse = getVideoData(videoId.trim());
            JsonNode videoJson = objectMapper.readTree(videoResponse);

            if (!videoJson.has("items") || videoJson.get("items").isEmpty()) {
                throw new BusinessException(VideoError.VIDEO_NOT_FOUND);
            }

            JsonNode videoItem = videoJson.get("items").get(0);
            String channelId = videoItem.get("snippet").get("channelId").asText();

            // 2. 채널 정보 조회
            String channelResponse = getChannelData(channelId);
            JsonNode channelJson = objectMapper.readTree(channelResponse);

            JsonNode channelItem = channelJson.get("items").get(0);

            // 3. 응답 생성
            VideoApiResponse response = buildVideoResponse(videoItem, channelItem);

            log.info("비디오 정보 조회 완료: apiVideoId={}", videoId);
            return response;

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("비디오를 찾을 수 없음: apiVideoId={}", videoId);
            throw new BusinessException(VideoError.VIDEO_NOT_FOUND);

        } catch (HttpClientErrorException.Forbidden e) {
            log.warn("비디오 접근 금지: apiVideoId={}", videoId);
            throw new IllegalStateException("이 비디오에 접근할 수 없습니다", e);

        } catch (RestClientException e) {
            log.error("YouTube API 호출 실패: apiVideoId={}, error={}", videoId, e.getMessage(), e);
            throw new BusinessException(CommonError.VIDEO_PROCESSING_ERROR);

        } catch (Exception e) {
            log.error("비디오 정보 조회 실패: apiVideoId={}, error={}", videoId, e.getMessage(), e);
            throw new BusinessException(CommonError.VIDEO_PROCESSING_ERROR);
        }
    }

    /**
     * DB에서 비디오 조회
     *
     * @param apiVideoId YouTube API 비디오 ID
     * @return 조회된 비디오 (Optional)
     */
    public Optional<Video> findByApiVideoId(String apiVideoId) {
        return videoRepository.findByApiVideoId(apiVideoId);
    }

    /**
     * AI 분석 완료 여부 체크
     *
     * @param video 확인할 비디오 엔티티
     * @return AI 분석 완료 여부
     */
    public boolean isAIAnalysisCompleted(Video video) {
        return video.getSummation() != null &&
                video.getLanguageDistribution() != null &&
                video.getSentimentDistribution() != null &&
                video.getKeywords() != null;
        // warning은 boolean이라 null 체크 안함
    }

    /**
     * YouTube API로 영상 삭제 여부 확인
     * @return true: 삭제됨/비공개, false: 정상
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

        } catch (HttpClientErrorException.NotFound e) {
            log.info("영상 삭제됨 (404): apiVideoId={}", apiVideoId);
            return true;
        } catch (HttpClientErrorException.Forbidden e) {
            log.info("영상 비공개 처리됨 (403): apiVideoId={}", apiVideoId);
            return true;
        } catch (Exception e) {
            log.error("영상 삭제 확인 중 오류: apiVideoId={}, error={}",
                    apiVideoId, e.getMessage());
            return false; // 오류 시 삭제 안 된 것으로 간주
        }
    }

    /**
     * 메타데이터만 업데이트 (조회수, 좋아요, 댓글 수)
     */
    @Transactional
    public void updateMetadataOnly(Long videoId, String apiVideoId) {
        try {
            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new BusinessException(VideoError.VIDEO_NOT_FOUND));

            VideoApiResponse latestInfo = getVideoInfo(apiVideoId);

            video.setViewCount(latestInfo.viewCount());
            video.setLikeCount(latestInfo.likeCount());
            video.setCommentCount(latestInfo.commentCount());
            video.setLastMetadataUpdatedAt(java.time.LocalDateTime.now());
            video.setMetadataUpdateCount(video.getMetadataUpdateCount() + 1);

            videoRepository.save(video);

            log.info("메타데이터 업데이트 완료: videoId={}, 업데이트 횟수={}",
                    videoId, video.getMetadataUpdateCount());

        } catch (Exception e) {
            log.error("메타데이터 업데이트 실패: videoId={}, error={}",
                    videoId, e.getMessage(), e);
            throw new BusinessException(CommonError.VIDEO_PROCESSING_ERROR);
        }
    }

    /**
     * 비디오와 댓글을 AI 분석 없이 저장
     *
     * @param videoApiResponse YouTube API로부터 받은 비디오 정보
     * @param commentInfo      댓글 분석 정보
     * @return 저장된 비디오의 데이터베이스 ID
     */
    @Transactional
    public Long saveVideoAndCommentsWithoutAI(VideoApiResponse videoApiResponse, CommentApiResponse commentInfo) {
        try {
            Video video = Video.builder()
                    .apiVideoId(videoApiResponse.apiVideoId())
                    .title(videoApiResponse.title())
                    .description(videoApiResponse.description())
                    .thumbnailUrl(videoApiResponse.thumbnailUrl())
                    .channelId(videoApiResponse.channelId())
                    .channelName(videoApiResponse.channelTitle())
                    .channelThumbnailUrl(videoApiResponse.channelThumbnailUrl())
                    .uploadedAt(videoApiResponse.publishedAt())
                    .subscriberCount(videoApiResponse.subscriberCount())
                    .viewCount(videoApiResponse.viewCount())
                    .likeCount(videoApiResponse.likeCount())
                    .commentCount(videoApiResponse.commentCount())
                    .commentHistogram(objectMapper.writeValueAsString(commentInfo.commentHistogram()))
                    .popularTimestamps(objectMapper.writeValueAsString(commentInfo.popularTimestamps()))
                    .summation(null)
                    .isWarning(false)
                    .languageDistribution(null)
                    .sentimentDistribution(null)
                    .keywords(null)
                    .commentsDisabled(false)
                    .hasNoComments(commentInfo.allComments().isEmpty())
                    .lastAiAttemptAt(null)
                    .aiRetryCount(0)
                    .aiProcessing(false)
                    .lastMetadataUpdatedAt(null)
                    .deleted(false)
                    .deleteCheckedAt(null)
                    .metadataUpdateCount(0)
                    .build();

            Video savedVideo = videoRepository.save(video);

            // 댓글이 있을 때만 저장
            if (!commentInfo.allComments().isEmpty()) {
                commentService.saveCommentsToDb(commentInfo.allComments(), savedVideo);
            }

            log.info("비디오와 댓글 저장 완료 (AI 분석 없이): apiVideoId={}, videoId={}, 댓글수={}",
                    videoApiResponse.apiVideoId(), savedVideo.getId(), commentInfo.allComments().size());

            return savedVideo.getId();

        } catch (Exception e) {
            log.error("비디오 저장 실패: apiVideoId={}, error={}",
                    videoApiResponse.apiVideoId(), e.getMessage());
            throw new BusinessException(CommonError.VIDEO_PROCESSING_ERROR);
        }
    }

    /**
     * AI 분석 결과로 비디오 업데이트 (null이 아닌 필드만)
     *
     * @param videoId          업데이트할 비디오의 데이터베이스 ID
     * @param analysisResponse AI 분석 결과
     */
    @Transactional
    public void updateWithAIResults(Long videoId, AIAnalysisResponse analysisResponse) {
        try {
            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new BusinessException(VideoError.VIDEO_NOT_FOUND));

            if (analysisResponse.summation() != null) {
                video.setSummation(analysisResponse.summation());
            }

            video.setWarning(analysisResponse.isWarning());

            if (analysisResponse.languageRatio() != null) {
                video.setLanguageDistribution(objectMapper.writeValueAsString(analysisResponse.languageRatio()));
            }

            if (analysisResponse.sentimentRatio() != null) {
                video.setSentimentDistribution(objectMapper.writeValueAsString(analysisResponse.sentimentRatio()));
            }

            if (analysisResponse.keywords() != null) {
                video.setKeywords(objectMapper.writeValueAsString(analysisResponse.keywords()));
            }

            videoRepository.save(video);

            log.info("AI 분석 결과 업데이트 완료: videoId={}", videoId);

        } catch (Exception e) {
            log.error("AI 분석 결과 업데이트 실패: videoId={}, error={}", videoId, e.getMessage());
            throw new BusinessException(CommonError.VIDEO_PROCESSING_ERROR);
        }
    }

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

        // 썸네일 URL 추출 (standard 우선, 없으면 high, 없으면 medium, 없으면 default)
        String thumbnailUrl = extractThumbnailUrl(snippet.get("thumbnails"));
        log.info("썸네일 url: {}", thumbnailUrl);
        String channelThumbnailUrl = extractThumbnailUrl(channelSnippet.get("thumbnails"));

        return new VideoApiResponse(
                videoItem.get("id").asText(),
                snippet.get("title").asText(),
                snippet.has("description") ? snippet.get("description").asText() : "",
                statistics.has("viewCount") ? statistics.get("viewCount").asText() : "0",
                statistics.has("likeCount") ? statistics.get("likeCount").asText() : "0",
                statistics.has("commentCount") ? statistics.get("commentCount").asText() : "0",                thumbnailUrl,
                snippet.get("channelId").asText(),
                snippet.get("channelTitle").asText(),
                channelThumbnailUrl,
                channelStatistics.has("subscriberCount") ? channelStatistics.get("subscriberCount").asText() : "0",
                snippet.get("publishedAt").asText()
        );
    }

    /**
     * 썸네일 URL 추출 (우선순위: standard > high > medium > default)
     *
     * @param thumbnails YouTube API의 thumbnails JSON 객체
     * @return 추출된 썸네일 URL (없으면 빈 문자열)
     */
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