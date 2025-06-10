package com.knu.sosuso.capstone.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.sosuso.capstone.ai.dto.AIAnalysisResponse;
import com.knu.sosuso.capstone.config.ApiConfig;
import com.knu.sosuso.capstone.domain.Video;
import com.knu.sosuso.capstone.dto.response.CommentApiResponse;
import com.knu.sosuso.capstone.dto.response.VideoApiResponse;
import com.knu.sosuso.capstone.repository.VideoRepository;
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
     * @param actualCommentCount 실제 수집된 댓글 수 (선택적)
     * @return 비디오 API 응답 객체
     */
    public VideoApiResponse getVideoInfo(String videoId, Integer actualCommentCount) {
        if (videoId == null || videoId.trim().isEmpty()) {
            throw new IllegalArgumentException("비디오 ID는 필수입니다");
        }

        try {
            log.info("비디오 정보 조회 시작: apiVideoId={}", videoId);

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

            log.info("비디오 정보 조회 완료: apiVideoId={}", videoId);
            return response;

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("비디오를 찾을 수 없음: apiVideoId={}", videoId);
            throw new IllegalArgumentException("존재하지 않는 비디오입니다", e);

        } catch (HttpClientErrorException.Forbidden e) {
            log.warn("비디오 접근 금지: apiVideoId={}", videoId);
            throw new IllegalStateException("이 비디오에 접근할 수 없습니다", e);

        } catch (RestClientException e) {
            log.error("YouTube API 호출 실패: apiVideoId={}, error={}", videoId, e.getMessage(), e);
            throw new RuntimeException("비디오 정보를 가져올 수 없습니다", e);

        } catch (Exception e) {
            log.error("비디오 정보 조회 실패: apiVideoId={}, error={}", videoId, e.getMessage(), e);
            throw new RuntimeException("비디오 정보 조회 중 오류 발생", e);
        }
    }

    /**
     * 댓글 수 업데이트
     *
     * @param videoApiResponse   기존 비디오 응답 객체
     * @param actualCommentCount 실제 수집된 댓글 수
     * @return 댓글 수가 업데이트된 비디오 응답 객체
     */
    public VideoApiResponse updateCommentCount(VideoApiResponse videoApiResponse, int actualCommentCount) {
        return new VideoApiResponse(
                videoApiResponse.apiVideoId(),
                videoApiResponse.title(),
                videoApiResponse.description(),
                videoApiResponse.viewCount(),
                videoApiResponse.likeCount(),
                String.valueOf(actualCommentCount),
                videoApiResponse.thumbnailUrl(),
                videoApiResponse.channelId(),
                videoApiResponse.channelTitle(),
                videoApiResponse.channelThumbnailUrl(),
                videoApiResponse.subscriberCount(),
                videoApiResponse.publishedAt()
        );
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
     * DB에서 비디오 조회 (ID로)
     *
     * @param id 비디오 데이터베이스 ID
     * @return 조회된 비디오 (Optional)
     */
    public Optional<Video> findById(Long id) {
        return videoRepository.findById(id);
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
     * 기존 데이터 삭제 (1일 지난 경우)
     *
     * @param videoId 삭제할 비디오의 데이터베이스 ID
     */
    @Transactional
    public void deleteExistingData(Long videoId) {
        try {
            commentService.deleteCommentsByVideoId(videoId);
            videoRepository.deleteById(videoId);
            log.info("기존 데이터 삭제 완료: videoId={}", videoId);
        } catch (Exception e) {
            log.error("기존 데이터 삭제 실패: videoId={}, error={}", videoId, e.getMessage());
            throw new RuntimeException("기존 데이터 삭제 중 오류 발생", e);
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
            // 1. 비디오 저장 (AI 필드들은 null)
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
                    // AI 필드들은 null로 저장
                    .summation(null)
                    .isWarning(false)
                    .languageDistribution(null)
                    .sentimentDistribution(null)
                    .keywords(null)
                    .build();

            Video savedVideo = videoRepository.save(video);

            // 2. 댓글 저장 (sentiment는 null)
            commentService.saveCommentsToDb(commentInfo.allComments(), savedVideo);

            log.info("비디오와 댓글 저장 완료 (AI 분석 없이): apiVideoId={}, videoId={}",
                    videoApiResponse.apiVideoId(), savedVideo.getId());

            return savedVideo.getId();

        } catch (Exception e) {
            log.error("비디오 저장 실패: apiVideoId={}, error={}",
                    videoApiResponse.apiVideoId(), e.getMessage());
            throw new RuntimeException("비디오 저장 중 오류 발생", e);
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
                    .orElseThrow(() -> new IllegalArgumentException("비디오를 찾을 수 없습니다: " + videoId));

            // null이 아닌 필드만 업데이트
            if (analysisResponse.summation() != null) {
                video.setSummation(analysisResponse.summation());
            }

            // boolean은 기본값이 false이므로 항상 업데이트
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
            throw new RuntimeException("AI 분석 결과 업데이트 중 오류 발생", e);
        }
    }

    /**
     * 기존 비디오에 댓글 분석 결과 업데이트 (백엔드 분석)
     *
     * @param videoId     업데이트할 비디오의 데이터베이스 ID
     * @param commentInfo 백엔드에서 분석한 댓글 정보
     */
    @Transactional
    public void updateVideoWithCommentAnalysis(Long videoId, CommentApiResponse commentInfo) {
        try {
            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new IllegalArgumentException("비디오를 찾을 수 없습니다: " + videoId));

            // 댓글 개수 업데이트
            video.setCommentCount(String.valueOf(commentInfo.allComments().size()));

            // 백엔드 분석 결과 업데이트
            video.setCommentHistogram(objectMapper.writeValueAsString(commentInfo.commentHistogram()));
            video.setPopularTimestamps(objectMapper.writeValueAsString(commentInfo.popularTimestamps()));

            videoRepository.save(video);
            log.info("기존 비디오 댓글 분석 결과 업데이트 완료: videoId={}", videoId);

        } catch (Exception e) {
            log.error("기존 비디오 댓글 분석 결과 업데이트 실패: videoId={}, error={}", videoId, e.getMessage());
            throw new RuntimeException("비디오 댓글 분석 업데이트 중 오류 발생", e);
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
     * @param actualCommentCount 실제 수집된 댓글 수 (선택적)
     * @return 변환된 VideoApiResponse 객체
     */
    private VideoApiResponse buildVideoResponse(JsonNode videoItem, JsonNode channelItem, Integer actualCommentCount) {
        JsonNode snippet = videoItem.get("snippet");
        JsonNode statistics = videoItem.get("statistics");
        JsonNode channelSnippet = channelItem.get("snippet");
        JsonNode channelStatistics = channelItem.get("statistics");

        // 썸네일 URL 추출 (standard 우선, 없으면 high, 없으면 medium, 없으면 default)
        String thumbnailUrl = extractThumbnailUrl(snippet.get("thumbnails"));
        log.info("썸네일 url: {}", thumbnailUrl);
        String channelThumbnailUrl = extractThumbnailUrl(channelSnippet.get("thumbnails"));

        String commentCount;
        if (actualCommentCount != null) {
            commentCount = String.valueOf(actualCommentCount);
        } else {
            commentCount = statistics.has("commentCount") ? statistics.get("commentCount").asText() : "0";
        }

        return new VideoApiResponse(
                videoItem.get("id").asText(),
                snippet.get("title").asText(),
                snippet.has("description") ? snippet.get("description").asText() : "",
                statistics.has("viewCount") ? statistics.get("viewCount").asText() : "0",
                statistics.has("likeCount") ? statistics.get("likeCount").asText() : "0",
                commentCount,
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