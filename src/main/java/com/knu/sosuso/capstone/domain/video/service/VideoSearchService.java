package com.knu.sosuso.capstone.domain.video.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.sosuso.capstone.domain.video.dto.response.SearchResultPageResponse;
import com.knu.sosuso.capstone.domain.video.dto.response.VideoSummaryResponse;
import com.knu.sosuso.capstone.domain.video.entity.Video;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * YouTube 검색 API 호출 및 Fast Path 처리 서비스
 * - 검색 결과를 DB에 즉시 저장 (AI 없이)
 * - 백그라운드에서 AI 분석 비동기 처리
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
     * Fast Path: DB 저장 → 즉시 응답 → 백그라운드 AI 분석
     */
    @Transactional
    public SearchResultPageResponse searchVideos(String token, String query, String pageToken) {
        log.info("동영상 검색 시작: query={}, pageToken={}", query, pageToken);

        if (query == null || query.trim().isEmpty()) {
            throw new BusinessException(SearchError.SEARCH_QUERY_REQUIRED);
        }

        try {
            // 1. YouTube API 호출 (쇼츠 제외)
            String searchResponse = callYouTubeSearchApi(
                    query.trim(),
                    pageToken,
                    "video",
                    "any" // 모든 길이 (쇼츠 포함 안함)
            );

            // 2. 검색 결과 파싱 및 DB 저장 (Fast Path)
            return processSearchResults(token, searchResponse, query, "VIDEO");

        } catch (HttpClientErrorException e) {
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

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("동영상 검색 실패: query={}, error={}", query, e.getMessage(), e);
            throw new BusinessException(SearchError.SEARCH_ERROR);
        }
    }

    /**
     * 쇼츠 검색
     * Fast Path: DB 저장 → 즉시 응답 → 백그라운드 AI 분석
     */
    @Transactional
    public SearchResultPageResponse searchShorts(String token, String query, String pageToken) {
        log.info("쇼츠 검색 시작: query={}, pageToken={}", query, pageToken);

        if (query == null || query.trim().isEmpty()) {
            throw new BusinessException(SearchError.SEARCH_QUERY_REQUIRED);
        }

        try {
            // 1. YouTube API 호출 (쇼츠만)
            String searchResponse = callYouTubeSearchApi(
                    query.trim(),
                    pageToken,
                    "video",
                    "short" // 쇼츠만 (4분 미만)
            );

            // 2. 검색 결과 파싱 및 DB 저장 (Fast Path)
            return processSearchResults(token, searchResponse, query, "SHORT");

        } catch (HttpClientErrorException e) {
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
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(YOUTUBE_SEARCH_API_URL)
                .queryParam("part", "snippet")
                .queryParam("type", type)
                .queryParam("q", query)
                .queryParam("maxResults", appConfig.getSearchResultsPerPage())
                .queryParam("relevanceLanguage", "ko")
                .queryParam("key", apiConfig.getKey());

        // videoDuration 파라미터 추가 (쇼츠 구분용)
        if (videoDuration != null && !videoDuration.isEmpty()) {
            builder.queryParam("videoDuration", videoDuration);
        }

        // pageToken이 있으면 추가
        if (pageToken != null && !pageToken.isEmpty()) {
            builder.queryParam("pageToken", pageToken);
        }

        String apiUrl = builder.build(false).toUriString();

        log.debug("YouTube Search API 호출: {}", apiUrl);
        return restTemplate.getForObject(apiUrl, String.class);
    }

    /**
     * 검색 결과 처리 (Fast Path)
     * 1. DB에 메타데이터만 즉시 저장 (AI 없이)
     * 2. VideoSummaryResponse 생성하여 즉시 응답
     * 3. 백그라운드에서 AI 분석 시작
     */
    private SearchResultPageResponse processSearchResults(String token, String searchResponse,
                                                          String query, String searchType) {
        try {
            JsonNode rootNode = objectMapper.readTree(searchResponse);
            JsonNode itemsNode = rootNode.path("items");
            String nextPageToken = rootNode.path("nextPageToken").asText(null);

            List<VideoSummaryResponse> results = new ArrayList<>();
            int skippedCount = 0;

            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    String kind = item.path("id").path("kind").asText();

                    if (!"youtube#video".equals(kind)) {
                        skippedCount++;
                        log.debug("비디오가 아닌 항목 스킵: kind={}", kind);
                        continue;
                    }

                    String apiVideoId = item.path("id").path("videoId").asText();

                    if (apiVideoId == null || apiVideoId.isEmpty()) {
                        continue;
                    }

                    try {
                        // Fast Path: DB 저장 및 응답 생성
                        VideoSummaryResponse videoResponse =
                                processSingleVideo(token, apiVideoId);

                        results.add(videoResponse);

                    } catch (Exception e) {
                        log.warn("개별 영상 처리 실패: apiVideoId={}, error={}",
                                apiVideoId, e.getMessage());
                        // 개별 실패는 무시하고 계속 진행
                    }
                }
            }

            boolean hasMore = nextPageToken != null && !nextPageToken.isEmpty();

            log.info("{} 검색 완료: query={}, 전체={}, 비디오={}, 스킵={}, hasMore={}",
                    searchType, query, itemsNode.size(), results.size(), skippedCount, hasMore);

            return new SearchResultPageResponse(
                    results,
                    nextPageToken,
                    results.size(),
                    hasMore
            );

        } catch (Exception e) {
            log.error("검색 결과 파싱 실패: {}", e.getMessage(), e);
            throw new BusinessException(CommonError.DATA_PARSING_ERROR);
        }
    }

    /**
     * 단일 영상 처리 (Fast Path)
     * 1. DB에 있는지 확인
     * 2. 없으면 YouTube API로 조회 → DB 저장 (AI 없이)
     * 3. VideoSummaryResponse 생성
     * 4. 백그라운드 AI 분석 시작
     */
    private VideoSummaryResponse processSingleVideo(String token, String apiVideoId) {
        // 1. DB에서 먼저 확인
        Optional<Video> existingVideo = videoRepository.findByApiVideoId(apiVideoId);

        if (existingVideo.isPresent()) {
            log.debug("DB에 이미 존재하는 영상: apiVideoId={}", apiVideoId);

            Video video = existingVideo.get();

            // AI 분석이 완료되지 않았으면 백그라운드 처리 (재시도 조건 확인)
            if (!isAICompleted(video) && !video.isCommentsDisabled() && !video.isHasNoComments()) {
                if (shouldRetryAI(video)) {
                    scheduleBackgroundAI(video.getId(), apiVideoId);
                } else {
                    log.debug("AI 재시도 쿨타임 중: videoId={}, lastAttempt={}",
                            video.getId(), video.getLastAiAttemptAt());
                }
            }

            return mapVideoToSummaryResponse(video);
        }

        // 2. DB에 없으면 새로 처리 (Fast Path)
        log.debug("새로운 영상, 처리 시작: apiVideoId={}", apiVideoId);

        // Fast Path: AI 분석 없이 메타데이터만 저장
        videoProcessingService.processVideoToSearchResult(token, apiVideoId, false);

        // 다시 DB에서 조회하여 응답 생성
        Video newVideo = videoRepository.findByApiVideoId(apiVideoId)
                .orElseThrow(() -> new BusinessException(CommonError.VIDEO_PROCESSING_ERROR));

        // 백그라운드에서 AI 분석 시작 (비동기)
        if (!newVideo.isCommentsDisabled() && !newVideo.isHasNoComments()) {
            scheduleBackgroundAI(newVideo.getId(), apiVideoId);
        }

        return mapVideoToSummaryResponse(newVideo);
    }

    /**
     * Video 엔티티를 VideoSummaryResponse로 변환
     */
    private VideoSummaryResponse mapVideoToSummaryResponse(Video video) {
        // Video 정보
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

        // Channel 정보
        VideoSummaryResponse.Channel channelDto = new VideoSummaryResponse.Channel(
                video.getChannelId(),
                video.getChannelName(),
                video.getChannelThumbnailUrl(),
                parseLong(video.getSubscriberCount())
        );

        // Analysis 정보 (AI 분석 완료 시에만)
        VideoSummaryResponse.Analysis analysisDto = null;
        if (isAICompleted(video)) {
            try {
                // 감정 분포 파싱
                JsonNode sentimentNode = objectMapper.readTree(video.getSentimentDistribution());
                VideoSummaryResponse.SentimentDistribution sentiment =
                        new VideoSummaryResponse.SentimentDistribution(
                                sentimentNode.path("positive").asDouble(0.0),
                                sentimentNode.path("negative").asDouble(0.0),
                                sentimentNode.path("other").asDouble(0.0)
                        );

                // 키워드 파싱
                JsonNode keywordsNode = objectMapper.readTree(video.getKeywords());
                List<String> keywords = new ArrayList<>();
                if (keywordsNode.isArray()) {
                    keywordsNode.forEach(node -> keywords.add(node.asText()));
                }

                analysisDto = new VideoSummaryResponse.Analysis(
                        video.getSummation(),
                        sentiment,
                        keywords
                );

            } catch (Exception e) {
                log.warn("AI 분석 데이터 파싱 실패: videoId={}, error={}",
                        video.getId(), e.getMessage());
                analysisDto = null;
            }
        }

        return new VideoSummaryResponse(videoDto, channelDto, analysisDto);
    }

    /**
     * String을 Long으로 안전하게 변환
     */
    private Long parseLong(String value) {
        try {
            return value != null && !value.isEmpty() ? Long.parseLong(value) : 0L;
        } catch (NumberFormatException e) {
            log.warn("Long 변환 실패: {}", value);
            return 0L;
        }
    }

    /**
     * String을 Integer로 안전하게 변환
     */
    private Integer parseInt(String value) {
        try {
            return value != null && !value.isEmpty() ? Integer.parseInt(value) : 0;
        } catch (NumberFormatException e) {
            log.warn("Integer 변환 실패: {}", value);
            return 0;
        }
    }

    /**
     * AI 분석 완료 여부 체크
     */
    private boolean isAICompleted(Video video) {
        return video.getSummation() != null &&
                video.getLanguageDistribution() != null &&
                video.getSentimentDistribution() != null &&
                video.getKeywords() != null;
    }

    /**
     * AI 재시도 가능 여부 체크
     * - AI 처리 중이 아니어야 함
     * - 마지막 시도 후 쿨타임(5분) 경과해야 함
     */
    private boolean shouldRetryAI(Video video) {
        // 이미 AI 처리 중이면 재시도 안 함
        if (video.isAiProcessing()) {
            return false;
        }

        // 마지막 시도 시간 확인
        LocalDateTime lastAttempt = video.getLastAiAttemptAt();
        if (lastAttempt == null) {
            return true; // 한 번도 시도 안 했으면 시도 가능
        }

        // 쿨타임 확인 (5분)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cooldownEnd = lastAttempt.plusMinutes(appConfig.getAiRetryCooldownMinutes());

        return now.isAfter(cooldownEnd);
    }

    /**
     * 백그라운드 AI 분석 스케줄링
     * VideoProcessingService의 @Async 메서드를 호출하여 비동기 처리
     */
    private void scheduleBackgroundAI(Long videoId, String apiVideoId) {
        try {
            log.info("백그라운드 AI 분석 스케줄: videoId={}, apiVideoId={}", videoId, apiVideoId);

            // VideoProcessingService의 비동기 백그라운드 AI 처리 메서드 호출
            videoProcessingService.scheduleBackgroundAIProcessing(videoId, apiVideoId);

            log.info("백그라운드 AI 처리 요청 완료: videoId={}", videoId);

        } catch (Exception e) {
            log.warn("백그라운드 AI 스케줄링 실패: videoId={}, error={}", videoId, e.getMessage());
        }
    }
}