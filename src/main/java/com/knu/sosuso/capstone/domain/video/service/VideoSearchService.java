package com.knu.sosuso.capstone.domain.video.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.sosuso.capstone.domain.video.dto.response.SearchResultPageResponse;
import com.knu.sosuso.capstone.domain.video.dto.response.VideoSummaryResponse;
import com.knu.sosuso.capstone.domain.video.entity.VideoType;
import com.knu.sosuso.capstone.global.config.ApiConfig;
import com.knu.sosuso.capstone.global.config.AppConfig;
import com.knu.sosuso.capstone.global.exception.BusinessException;
import com.knu.sosuso.capstone.global.exception.error.CommonError;
import com.knu.sosuso.capstone.global.exception.error.SearchError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * YouTube 검색 서비스 (Shorts URL 체크 기반 - 가장 정확!)
 *
 * ✅ 핵심 전략:
 * 1. Search API로 videoId 수집 (빠름)
 * 2. Shorts URL 병렬 체크로 타입 판별 (정확 + 빠름)
 * 3. 타입별로 분류 후 필요한 것만 처리
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class VideoSearchService {

    private static final String YOUTUBE_SEARCH_API_URL = "https://www.googleapis.com/youtube/v3/search";
    private static final String YOUTUBE_VIDEOS_API_URL = "https://www.googleapis.com/youtube/v3/videos";

    private final ApiConfig apiConfig;
    private final AppConfig appConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final VideoProcessingService videoProcessingService;

    // URL 체크 전용 스레드풀 주입
    @Qualifier("urlCheckExecutor")
    private final Executor urlCheckExecutor;

    // 검색 처리 전용 스레드풀 주입
    @Qualifier("searchProcessingExecutor")
    private final Executor searchProcessingExecutor;

    /**
     * 동영상 검색 (쇼츠 제외)
     */
    @Cacheable(value = "searchResults", key = "'video-' + #query + '-' + #pageToken", unless = "#result.results.isEmpty()")
    public SearchResultPageResponse searchVideos(String token, String query, String pageToken) {
        log.info("🔍 동영상 검색 (캐시 미스): query={}, pageToken={}", query, pageToken);
        return searchWithUrlCheck(token, query, pageToken, VideoType.VIDEO);
    }

    /**
     * 쇼츠 검색
     */
    @Cacheable(value = "searchResults", key = "'shorts-' + #query + '-' + #pageToken", unless = "#result.results.isEmpty()")
    public SearchResultPageResponse searchShorts(String token, String query, String pageToken) {
        log.info("🔍 쇼츠 검색 (캐시 미스): query={}, pageToken={}", query, pageToken);
        return searchWithUrlCheck(token, query, pageToken, VideoType.SHORTS);
    }

    /**
     * Shorts URL 체크 기반 검색 로직
     *
     * 전략:
     * 1. Search API로 videoId 대량 수집 - 외부 HTTP
     * 2. Shorts URL 병렬 체크로 타입 판별 - 외부 HTTP
     * 3. 타입별로 분류 후 필요한 것만 처리 - DB 작업만 트랜잭션
     * 4. 4개 채울 때까지 반복
     */
    private SearchResultPageResponse searchWithUrlCheck(
            String token, String query, String pageToken, VideoType targetType) {

        List<VideoSummaryResponse> results = new ArrayList<>();
        String currentPageToken = pageToken;
        int retryCount = 0;
        int maxRetries = appConfig.getMaxRetryPages(); // 3

        while (results.size() < 4 && retryCount < maxRetries) {
            log.info("🔄 검색 시도 {}/{}: targetType={}, 현재 결과={}/4",
                    retryCount + 1, maxRetries, targetType, results.size());

            // 1. Search API 호출 (대량 수집)
            SearchBatchResult searchResult = callSearchAPI(query, currentPageToken);

            if (searchResult.videoIds.isEmpty()) {
                log.info("❌ Search API 결과 없음 - 검색 종료");
                break;
            }

            log.info("📋 Search API 결과: {}개 videoId 받음", searchResult.videoIds.size());

            // 2. Shorts URL 병렬 체크로 타입 분류
            TypedResult typedResult = classifyByParallelUrlCheck(searchResult.videoIds);

            log.info("🎯 URL 체크 완료: 영상={}개, 쇼츠={}개",
                    typedResult.videoIds.size(), typedResult.shortsIds.size());

            // 3. 원하는 타입만 추출
            List<String> targetIds = targetType == VideoType.VIDEO
                    ? typedResult.videoIds
                    : typedResult.shortsIds;

            if (targetIds.isEmpty()) {
                log.info("⚠️ 원하는 타입({}) 없음 - 다음 페이지 시도", targetType);
                currentPageToken = searchResult.nextPageToken;
                retryCount++;
                continue;
            }

            // 4. 필요한 만큼만 처리 (최대 4개)
            int needed = 4 - results.size();
            List<String> idsToProcess = targetIds.subList(
                    0, Math.min(needed, targetIds.size())
            );

            // 5. Videos API로 배치 처리
            List<VideoSummaryResponse> batch = processVideosBatchWithTransaction(
                    token, idsToProcess, targetType
            );
            results.addAll(batch);

            log.info("✅ 배치 처리 완료: {}개 추가, 현재 총 {}/4",
                    batch.size(), results.size());

            // 6. 4개 채웠으면 백그라운드로 나머지 처리
            if (results.size() >= 4) {
                if (targetIds.size() > needed) {
                    List<String> remaining = targetIds.subList(needed, targetIds.size());
                    processVideosAsync(token, remaining, targetType);
                }
                break;
            }

            // 7. 다음 페이지로
            currentPageToken = searchResult.nextPageToken;
            retryCount++;

            if (currentPageToken == null) {
                log.info("🏁 더 이상 검색 결과 없음");
                break;
            }
        }

        // 8. 응답 생성
        boolean hasMore = currentPageToken != null && results.size() >= 4;

        log.info("🎯 검색 완료: query={}, targetType={}, 결과={}개, hasMore={}",
                query, targetType, results.size(), hasMore);

        return new SearchResultPageResponse(
                results,
                hasMore ? currentPageToken : null,
                results.size(),
                hasMore
        );
    }

    /**
     * Shorts URL 병렬 체크로 타입 분류
     */
    private TypedResult classifyByParallelUrlCheck(List<String> videoIds) {
        long startTime = System.currentTimeMillis();

        log.info("🚀 Shorts URL 병렬 체크 시작: {}개 영상", videoIds.size());

        // 전용 스레드풀로 병렬 처리
        List<CompletableFuture<VideoTypeResult>> futures = videoIds.stream()
                .map(videoId -> CompletableFuture.supplyAsync(() ->
                                new VideoTypeResult(videoId, checkIfShorts(videoId)),
                        urlCheckExecutor  // URL 체크 전용 스레드풀
                ))
                .collect(Collectors.toList());

        // 모든 작업 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 결과 수집 및 분류
        List<String> regularVideoIds = new ArrayList<>();
        List<String> shortsIds = new ArrayList<>();

        for (CompletableFuture<VideoTypeResult> future : futures) {
            try {
                VideoTypeResult result = future.get();
                if (result.isShorts) {
                    shortsIds.add(result.videoId);
                } else {
                    regularVideoIds.add(result.videoId);
                }
            } catch (Exception e) {
                log.warn("타입 체크 실패, 기본값 VIDEO로 처리: {}", e.getMessage());
            }
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        log.info("✅ Shorts URL 병렬 체크 완료: {}ms, 영상={}개, 쇼츠={}개",
                elapsedTime, regularVideoIds.size(), shortsIds.size());

        return new TypedResult(regularVideoIds, shortsIds);
    }

    /**
     * Shorts URL 체크 (단일 영상)
     *
     * https://www.youtube.com/shorts/{videoId}
     * → 200 OK: true (쇼츠)
     * → 404/301: false (일반 영상)
     */
    private boolean checkIfShorts(String videoId) {
        try {
            String shortsUrl = "https://www.youtube.com/shorts/" + videoId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    shortsUrl,
                    HttpMethod.HEAD,
                    entity,
                    String.class
            );

            // 200 OK면 Shorts
            boolean isShorts = response.getStatusCode() == HttpStatus.OK;
            log.debug("URL 체크: videoId={}, isShorts={}", videoId, isShorts);
            return isShorts;

        } catch (Exception e) {
            // 404, 301 등 모두 일반 영상으로 간주
            log.debug("URL 체크 실패 (일반 영상으로 판단): videoId={}", videoId);
            return false;
        }
    }

    /**
     * Videos API 배치 처리 + DB 저장
     */
    private List<VideoSummaryResponse> processVideosBatchWithTransaction(
            String token, List<String> videoIds, VideoType confirmedType) {

        if (videoIds.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("🔧 배치 처리 시작: {}개 영상, confirmedType={}", videoIds.size(), confirmedType);

        try {
            // 1. Videos API 호출 (외부 HTTP - 트랜잭션 밖)
            List<VideoData> videoDataList = fetchVideosInBatch(videoIds);

            // 2. DB 저장 (트랜잭션 적용)
            return processVideoDataListWithTransaction(token, videoDataList, confirmedType);

        } catch (Exception e) {
            log.error("❌ 배치 처리 실패: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * DB 저장 작업만 트랜잭션 적용
     */
    @Transactional
    public List<VideoSummaryResponse> processVideoDataListWithTransaction(
            String token, List<VideoData> videoDataList, VideoType confirmedType) {

        List<VideoSummaryResponse> results = new ArrayList<>();

        for (VideoData data : videoDataList) {
            try {
                // redetectType=false로 타입 재판별 방지
                VideoSummaryResponse summary = videoProcessingService
                        .processAndGetSummary(
                                token,
                                data.apiVideoId,
                                data.thumbnails,
                                confirmedType,
                                false  // ← 재판별 안 함!
                        );

                if (summary != null) {
                    results.add(summary);
                }

            } catch (Exception e) {
                log.warn("영상 처리 실패: apiVideoId={}, error={}",
                        data.apiVideoId, e.getMessage());
            }
        }

        log.info("✅ DB 저장 완료: 입력={}개, 성공={}개", videoDataList.size(), results.size());
        return results;
    }

    /**
     * 비동기 배치 처리 (전용 스레드풀 사용)
     */
    @Async("searchProcessingExecutor")
    public void processVideosAsync(String token, List<String> videoIds, VideoType targetType) {
        log.info("🔄 백그라운드 처리 시작: {}개 영상", videoIds.size());
        processVideosBatchWithTransaction(token, videoIds, targetType);
        log.info("✅ 백그라운드 처리 완료: {}개 영상", videoIds.size());
    }

    /**
     * YouTube Search API 호출
     */
    private SearchBatchResult callSearchAPI(String query, String pageToken) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(YOUTUBE_SEARCH_API_URL)
                    .queryParam("part", "snippet")
                    .queryParam("q", query)
                    .queryParam("type", "video")
                    .queryParam("maxResults", appConfig.getSearchBatchSize())
                    .queryParam("key", apiConfig.getKey());

            if (pageToken != null && !pageToken.isEmpty()) {
                builder.queryParam("pageToken", pageToken);
            }

            String apiUrl = builder.build(false).toUriString();
            log.debug("Search API 호출: query={}, maxResults={}", query, appConfig.getSearchBatchSize());

            String response = restTemplate.getForObject(apiUrl, String.class);
            return parseSearchResponse(response);

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.error("YouTube API 접근 거부");
                throw new BusinessException(SearchError.YOUTUBE_API_ACCESS_DENIED);
            }
            throw new BusinessException(SearchError.YOUTUBE_API_ERROR);

        } catch (Exception e) {
            log.error("Search API 호출 중 에러", e);
            throw new BusinessException(CommonError.YOUTUBE_API_ERROR);
        }
    }

    /**
     * Videos API 배치 호출 (트랜잭션 불필요)
     */
    private List<VideoData> fetchVideosInBatch(List<String> videoIds) {
        try {
            String ids = String.join(",", videoIds);

            String apiUrl = UriComponentsBuilder
                    .fromUriString(YOUTUBE_VIDEOS_API_URL)
                    .queryParam("part", "snippet,statistics")
                    .queryParam("id", ids)
                    .queryParam("key", apiConfig.getKey())
                    .build(false)
                    .toUriString();

            log.debug("Videos API 호출: {}개 영상", videoIds.size());

            String response = restTemplate.getForObject(apiUrl, String.class);
            return parseVideosResponse(response);

        } catch (Exception e) {
            log.error("Videos API 호출 실패", e);
            throw new BusinessException(SearchError.YOUTUBE_API_ERROR);
        }
    }

    /**
     * Videos API 응답 파싱
     */
    private List<VideoData> parseVideosResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode items = root.path("items");

            List<VideoData> results = new ArrayList<>();
            for (JsonNode item : items) {
                String apiVideoId = item.path("id").asText();
                JsonNode snippet = item.path("snippet");
                JsonNode thumbnails = snippet.path("thumbnails");
                results.add(new VideoData(apiVideoId, snippet, thumbnails));
            }
            return results;

        } catch (Exception e) {
            log.error("Videos API 응답 파싱 실패", e);
            throw new BusinessException(CommonError.DATA_PARSING_ERROR);
        }
    }

    /**
     * Search API 응답 파싱
     */
    private SearchBatchResult parseSearchResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode items = root.path("items");
            String nextPageToken = root.path("nextPageToken").asText(null);

            List<String> videoIds = new ArrayList<>();
            for (JsonNode item : items) {
                String kind = item.path("id").path("kind").asText();
                if ("youtube#video".equals(kind)) {
                    videoIds.add(item.path("id").path("videoId").asText());
                }
            }

            return new SearchBatchResult(videoIds, nextPageToken);

        } catch (Exception e) {
            log.error("Search API 응답 파싱 실패", e);
            throw new BusinessException(CommonError.DATA_PARSING_ERROR);
        }
    }

    // ==================== 내부 클래스 ====================

    /**
     * Search API 결과
     */
    private static class SearchBatchResult {
        final List<String> videoIds;
        final String nextPageToken;

        SearchBatchResult(List<String> videoIds, String nextPageToken) {
            this.videoIds = videoIds != null ? videoIds : Collections.emptyList();
            this.nextPageToken = nextPageToken;
        }
    }

    /**
     * 타입별로 분류된 결과
     */
    private static class TypedResult {
        final List<String> videoIds;   // 일반 영상
        final List<String> shortsIds;  // 쇼츠

        TypedResult(List<String> videoIds, List<String> shortsIds) {
            this.videoIds = videoIds != null ? videoIds : Collections.emptyList();
            this.shortsIds = shortsIds != null ? shortsIds : Collections.emptyList();
        }
    }

    /**
     * Videos API 응답
     */
    private static class VideoData {
        final String apiVideoId;
        final JsonNode snippet;
        final JsonNode thumbnails;

        VideoData(String apiVideoId, JsonNode snippet, JsonNode thumbnails) {
            this.apiVideoId = apiVideoId;
            this.snippet = snippet;
            this.thumbnails = thumbnails;
        }
    }

    /**
     * URL 체크 결과
     */
    private static class VideoTypeResult {
        final String videoId;
        final boolean isShorts;

        VideoTypeResult(String videoId, boolean isShorts) {
            this.videoId = videoId;
            this.isShorts = isShorts;
        }
    }
}