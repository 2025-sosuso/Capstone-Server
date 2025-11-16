package com.knu.sosuso.capstone.domain.video.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.sosuso.capstone.domain.video.dto.response.SearchResultPageResponse;
import com.knu.sosuso.capstone.domain.video.dto.response.VideoSummaryResponse;
import com.knu.sosuso.capstone.domain.video.entity.AIAnalysisStatus;
import com.knu.sosuso.capstone.domain.video.entity.Video;
import com.knu.sosuso.capstone.domain.video.entity.VideoType;
import com.knu.sosuso.capstone.domain.video.repository.VideoRepository;
import com.knu.sosuso.capstone.global.config.ApiConfig;
import com.knu.sosuso.capstone.global.config.AppConfig;
import com.knu.sosuso.capstone.global.exception.BusinessException;
import com.knu.sosuso.capstone.global.exception.error.CommonError;
import com.knu.sosuso.capstone.global.exception.error.SearchError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * YouTube 검색 서비스 (단순화 버전)
 * - 검색 시 AI 요청 없음
 * - 즉시 응답만 반환
 * - AI 분석은 스케줄러가 자동 처리
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class VideoSearchService {

    private static final String YOUTUBE_SEARCH_API_URL = "https://www.googleapis.com/youtube/v3/search";

    private final ApiConfig apiConfig;
    private final AppConfig appConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final VideoRepository videoRepository;
    private final VideoProcessingService videoProcessingService;

    /**
     * 동영상 검색 (쇼츠 제외)
     */
    @Transactional
    public SearchResultPageResponse searchVideos(String token, String query, String pageToken) {
        log.info("동영상 검색 시작: query={}, pageToken={}", query, pageToken);

        if (query == null || query.trim().isEmpty()) {
            throw new BusinessException(SearchError.SEARCH_QUERY_REQUIRED);
        }

        try {
            // YouTube API 호출
            String searchResponse = callYouTubeSearchApi(
                    query.trim(), pageToken, "video", "any"
            );

            // 검색 결과 처리
            return processSearchResults(token, searchResponse, query, VideoType.VIDEO);

        } catch (HttpClientErrorException e) {
            return handleHttpError(e, query);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("동영상 검색 실패: query={}, error={}", query, e.getMessage(), e);
            throw new BusinessException(SearchError.SEARCH_ERROR);
        }
    }

    /**
     * 쇼츠 검색
     */
    @Transactional
    public SearchResultPageResponse searchShorts(String token, String query, String pageToken) {
        log.info("쇼츠 검색 시작: query={}, pageToken={}", query, pageToken);

        if (query == null || query.trim().isEmpty()) {
            throw new BusinessException(SearchError.SEARCH_QUERY_REQUIRED);
        }

        try {
            // YouTube API 호출 (짧은 영상만)
            String searchResponse = callYouTubeSearchApi(
                    query.trim(), pageToken, "video", "short"
            );

            // 검색 결과 처리
            return processSearchResults(token, searchResponse, query, VideoType.SHORTS);

        } catch (HttpClientErrorException e) {
            return handleHttpError(e, query);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("쇼츠 검색 실패: query={}, error={}", query, e.getMessage(), e);
            throw new BusinessException(SearchError.SEARCH_ERROR);
        }
    }

    /**
     * YouTube Search API 호출
     */
    private String callYouTubeSearchApi(String query, String pageToken,
                                        String type, String videoDuration) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(YOUTUBE_SEARCH_API_URL)
                .queryParam("part", "snippet")
                .queryParam("type", type)
                .queryParam("q", query)
                .queryParam("maxResults", appConfig.getSearchResultsPerPage())
                .queryParam("relevanceLanguage", "ko")
                .queryParam("key", apiConfig.getKey());

        // 쇼츠 필터
        if (videoDuration != null && !videoDuration.isEmpty()) {
            builder.queryParam("videoDuration", videoDuration);
        }

        // 페이지 토큰
        if (pageToken != null && !pageToken.isEmpty()) {
            builder.queryParam("pageToken", pageToken);
        }

        String apiUrl = builder.build(false).toUriString();
        log.debug("YouTube API 호출: maxResults={}", appConfig.getSearchResultsPerPage());

        return restTemplate.getForObject(apiUrl, String.class);
    }

    /**
     * 검색 결과 처리
     * - DB 저장 (AI 없이)
     * - 즉시 응답 생성
     */
    private SearchResultPageResponse processSearchResults(String token, String searchResponse,
                                                          String query, VideoType videoType) {
        try {
            JsonNode rootNode = objectMapper.readTree(searchResponse);
            JsonNode itemsNode = rootNode.path("items");
            String nextPageToken = rootNode.path("nextPageToken").asText(null);

            List<VideoSummaryResponse> results = new ArrayList<>();
            int skippedCount = 0;

            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    // 비디오만 처리
                    String kind = item.path("id").path("kind").asText();
                    if (!"youtube#video".equals(kind)) {
                        skippedCount++;
                        continue;
                    }

                    String apiVideoId = item.path("id").path("videoId").asText();
                    if (apiVideoId == null || apiVideoId.isEmpty()) {
                        continue;
                    }

                    try {
                        // Fast Path: DB 저장 및 응답 생성
                        VideoSummaryResponse videoResponse = processSingleVideo(token, apiVideoId, videoType);
                        results.add(videoResponse);

                    } catch (Exception e) {
                        log.warn("개별 영상 처리 실패: apiVideoId={}, error={}",
                                apiVideoId, e.getMessage());
                    }
                }
            }

            boolean hasMore = nextPageToken != null && !nextPageToken.isEmpty();

            log.info("검색 완료: query={}, 결과={}개, hasMore={}", query, results.size(), hasMore);

            return new SearchResultPageResponse(results, nextPageToken, results.size(), hasMore);

        } catch (Exception e) {
            log.error("검색 결과 파싱 실패: {}", e.getMessage(), e);
            throw new BusinessException(CommonError.DATA_PARSING_ERROR);
        }
    }

    /**
     * 단일 영상 처리
     */
    private VideoSummaryResponse processSingleVideo(String token, String apiVideoId,
                                                    VideoType videoType) {
        Optional<Video> existingVideo = videoRepository.findByApiVideoId(apiVideoId);

        if (existingVideo.isPresent()) {
            Video video = existingVideo.get();

            // DB에 타입 없으면 업데이트
            if (video.getVideoType() == null) {
                video.setVideoType(videoType);
                videoRepository.save(video);
            }

            return mapVideoToSummaryResponse(video);
        }

        log.info("새 영상 처리: apiVideoId={}, type={}", apiVideoId, videoType);

        Video newVideo = videoProcessingService.processNewVideo(apiVideoId, videoType);

        log.info("새 영상 저장 완료: videoId={}, status={}",
                newVideo.getId(), newVideo.getAiAnalysisStatus());

        return mapVideoToSummaryResponse(newVideo);
    }

    /**
     * Video → VideoSummaryResponse 변환
     */
    private VideoSummaryResponse mapVideoToSummaryResponse(Video video) {
        VideoSummaryResponse.Video videoDto = new VideoSummaryResponse.Video(
                video.getApiVideoId(),
                video.getTitle(),
                video.getDescription(),
                video.getUploadedAt(),
                video.getThumbnailUrl(),
                parseLong(video.getViewCount()),
                parseLong(video.getLikeCount()),
                parseInt(video.getCommentCount())
        );

        VideoSummaryResponse.Channel channelDto = new VideoSummaryResponse.Channel(
                video.getChannelId(),
                video.getChannelName(),
                video.getChannelThumbnailUrl(),
                parseLong(video.getSubscriberCount())
        );

        VideoSummaryResponse.SentimentDistribution sentimentDist = null;
        List<String> keywords = null;
        String summary = null;

        // AI 완료된 경우에만 데이터 파싱
        if (video.getAiAnalysisStatus() == AIAnalysisStatus.COMPLETED) {
            summary = video.getSummation();

            // 감정 분포 파싱
            try {
                if (video.getSentimentDistribution() != null && !video.getSentimentDistribution().isEmpty()) {
                    Map<String, Double> sentimentMap = objectMapper.readValue(
                            video.getSentimentDistribution(),
                            objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Double.class)
                    );
                    sentimentDist = new VideoSummaryResponse.SentimentDistribution(
                            sentimentMap.getOrDefault("POSITIVE", 0.0),
                            sentimentMap.getOrDefault("NEGATIVE", 0.0),
                            sentimentMap.getOrDefault("OTHER", 0.0)
                    );
                }
            } catch (Exception e) {
                log.warn("감정 분포 파싱 실패: apiVideoId={}", video.getApiVideoId());
            }

            // 키워드 파싱
            try {
                if (video.getKeywords() != null && !video.getKeywords().isEmpty()) {
                    keywords = objectMapper.readValue(
                            video.getKeywords(),
                            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
                    );
                }
            } catch (Exception e) {
                log.warn("키워드 파싱 실패: apiVideoId={}", video.getApiVideoId());
            }
        }

        VideoSummaryResponse.Analysis analysisDto = new VideoSummaryResponse.Analysis(
                summary,
                sentimentDist,
                keywords != null ? keywords : List.of()
        );

        return new VideoSummaryResponse(videoDto, channelDto, analysisDto);
    }

    /**
     * HTTP 에러 처리
     */
    private SearchResultPageResponse handleHttpError(HttpClientErrorException e, String query) {
        if (e.getStatusCode().value() == 403) {
            String responseBody = e.getResponseBodyAsString();

            if (responseBody.contains("quotaExceeded")) {
                log.error("YouTube API quota 초과: query={}", query);
                throw new BusinessException(SearchError.YOUTUBE_API_QUOTA_EXCEEDED);
            }

            if (responseBody.contains("IP address restriction")) {
                log.error("YouTube API IP 제한: query={}", query);
                throw new BusinessException(SearchError.YOUTUBE_API_ACCESS_DENIED);
            }

            log.error("YouTube API 403 에러: query={}, error={}", query, responseBody);
            throw new BusinessException(SearchError.YOUTUBE_API_ACCESS_DENIED);
        }

        log.error("YouTube API 클라이언트 에러: query={}, status={}", query, e.getStatusCode());
        throw new BusinessException(SearchError.YOUTUBE_API_ERROR);
    }

    // 유틸리티 메서드
    private long parseLong(String value) {
        try {
            return value != null ? Long.parseLong(value) : 0L;
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private int parseInt(String value) {
        try {
            return value != null ? Integer.parseInt(value) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}