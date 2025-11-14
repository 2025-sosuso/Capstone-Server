package com.knu.sosuso.capstone.domain.video.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.sosuso.capstone.domain.ai.dto.AIAnalysisResponse;
import com.knu.sosuso.capstone.domain.comment.service.CommentService;
import com.knu.sosuso.capstone.domain.video.dto.response.VideoApiResponse;
import com.knu.sosuso.capstone.domain.video.entity.Video;
import com.knu.sosuso.capstone.domain.video.entity.VideoStatusHelper;
import com.knu.sosuso.capstone.domain.video.entity.value.AIAnalysisStatus;
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
        if (url == null || url.isEmpty()) {
            return null;
        }

        // URL에 youtube.com이나 youtu.be가 포함되지 않으면 비디오 ID로 간주
        if (!url.contains("youtube.com") && !url.contains("youtu.be") && !url.contains("m.youtube.com")) {
            // 11자리 영문자/숫자/-/_ 패턴 검증
            if (url.matches("[\\w-]{11}")) {
                return url;
            }
            return null;
        }

        Matcher matcher = VIDEO_ID_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 비디오 ID로 DB에서 비디오 조회
     *
     * @param apiVideoId YouTube 비디오 ID
     * @return Optional<Video> 객체
     */
    public Optional<Video> findByApiVideoId(String apiVideoId) {
        return videoRepository.findByApiVideoId(apiVideoId);
    }

    /**
     * YouTube API로 비디오 정보 조회
     *
     * @param apiVideoId YouTube 비디오 ID
     * @return VideoApiResponse 객체
     * @throws BusinessException 비디오를 찾을 수 없는 경우
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
     * AI 분석 완료 여부 확인 (AIAnalysisStatus 기반)
     *
     * @param video 비디오 엔티티
     * @return AI 분석 완료 여부
     */
    public boolean isAIAnalysisCompleted(Video video) {
        // AIAnalysisStatus로 확인
        if (video.getAiAnalysisStatus() != null) {
            return video.getAiAnalysisStatus() == AIAnalysisStatus.COMPLETED;
        }

        // Fallback: 기존 필드 확인 (하위 호환성)
        return video.getSummation() != null &&
                video.getLanguageDistribution() != null &&
                video.getSentimentDistribution() != null &&
                video.getKeywords() != null;
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

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().is4xxClientError()) {
                // API 키 문제 등
                log.error("YouTube API 에러: {}", e.getMessage());
                return false; // 삭제로 판단하지 않음
            }
            throw e;
        } catch (Exception e) {
            log.error("영상 삭제 여부 확인 실패: apiVideoId={}, error={}", apiVideoId, e.getMessage());
            return false; // 기본적으로 삭제되지 않은 것으로 처리
        }
    }

    /**
     * 영상 및 댓글 정보를 DB에 저장 (AI 분석 없이)
     *
     * @param videoApiResponse YouTube API로부터 받은 비디오 정보
     * @param commentInfo      백엔드에서 분석한 댓글 정보
     * @return 저장된 비디오의 데이터베이스 ID
     */
    @Transactional
    public Long saveVideoAndCommentsWithoutAI(VideoApiResponse videoApiResponse, CommentApiResponse commentInfo) {
        try {
            // 1. 비디오 엔티티 생성 및 저장
            Video video = Video.builder()
                    .apiVideoId(videoApiResponse.apiVideoId())
                    .title(videoApiResponse.title())
                    .description(videoApiResponse.description())
                    .viewCount(videoApiResponse.viewCount())
                    .likeCount(videoApiResponse.likeCount())
                    .commentCount(videoApiResponse.commentCount())
                    .thumbnailUrl(videoApiResponse.thumbnailUrl())
                    .channelId(videoApiResponse.channelId())
                    .channelName(videoApiResponse.channelTitle())
                    .channelThumbnailUrl(videoApiResponse.channelThumbnailUrl())
                    .subscriberCount(videoApiResponse.subscriberCount())
                    .uploadedAt(videoApiResponse.publishedAt())
                    // 백엔드 분석 정보 저장
                    .commentHistogram(objectMapper.writeValueAsString(commentInfo.commentHistogram()))
                    .popularTimestamps(objectMapper.writeValueAsString(commentInfo.popularTimestamps()))
                    // 댓글 상태
                    .commentsDisabled(false)
                    .hasNoComments(commentInfo.allComments().isEmpty())
                    // AI 상태 초기화
                    .aiAnalysisStatus(AIAnalysisStatus.PENDING)
                    .aiProcessing(false)
                    .aiRetryCount(0)
                    .build();

            Video savedVideo = videoRepository.save(video);

            // 2. 댓글이 있을 때만 저장
            if (!commentInfo.allComments().isEmpty()) {
                commentService.saveCommentsToDb(commentInfo.allComments(), savedVideo);
            } else {
                // 댓글이 없는 경우 AI 상태를 SKIPPED로 변경
                VideoStatusHelper.skipAIAnalysis(savedVideo, "No comments available");
                videoRepository.save(savedVideo);
            }

            log.info("비디오와 댓글 저장 완료 (AI 분석 없이): apiVideoId={}, videoId={}, 댓글수={}, aiStatus={}",
                    videoApiResponse.apiVideoId(), savedVideo.getId(), commentInfo.allComments().size(),
                    savedVideo.getAiAnalysisStatus());

            return savedVideo.getId();

        } catch (Exception e) {
            log.error("비디오 저장 실패: apiVideoId={}, error={}",
                    videoApiResponse.apiVideoId(), e.getMessage());
            throw new BusinessException(CommonError.VIDEO_PROCESSING_ERROR);
        }
    }

    /**
     * AI 분석 결과로 비디오 업데이트 (AIAnalysisStatus 포함)
     *
     * @param videoId          업데이트할 비디오의 데이터베이스 ID
     * @param analysisResponse AI 분석 결과
     */
    @Transactional
    public void updateWithAIResults(Long videoId, AIAnalysisResponse analysisResponse) {
        try {
            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new BusinessException(VideoError.VIDEO_NOT_FOUND));

            // AI 분석 결과 업데이트
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

            // AI 상태 업데이트 - 모든 필드가 성공적으로 업데이트되었는지 확인
            if (video.getSummation() != null &&
                    video.getLanguageDistribution() != null &&
                    video.getSentimentDistribution() != null &&
                    video.getKeywords() != null) {
                VideoStatusHelper.completeAIAnalysis(video);
            } else {
                VideoStatusHelper.partialCompleteAIAnalysis(video);
            }

            videoRepository.save(video);

            log.info("AI 분석 결과 업데이트 완료: videoId={}, aiStatus={}",
                    videoId, video.getAiAnalysisStatus());

        } catch (Exception e) {
            log.error("AI 분석 결과 업데이트 실패: videoId={}, error={}", videoId, e.getMessage());

            // 실패 시 상태 업데이트 시도
            try {
                Video video = videoRepository.findById(videoId).orElse(null);
                if (video != null) {
                    VideoStatusHelper.failAIAnalysis(video, e.getMessage());
                    videoRepository.save(video);
                }
            } catch (Exception ex) {
                log.error("AI 상태 업데이트 실패: {}", ex.getMessage());
            }

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
                statistics.has("commentCount") ? statistics.get("commentCount").asText() : "0",
                thumbnailUrl,
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