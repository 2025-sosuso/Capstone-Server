package com.knu.sosuso.capstone.domain.video.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.sosuso.capstone.domain.video.dto.response.SearchResultPageResponse;
import com.knu.sosuso.capstone.domain.video.dto.response.VideoSummaryResponse;
import com.knu.sosuso.capstone.domain.video.entity.Video;
import com.knu.sosuso.capstone.domain.video.entity.VideoType;
import com.knu.sosuso.capstone.domain.video.repository.VideoRepository;
import com.knu.sosuso.capstone.global.config.ApiConfig;
import com.knu.sosuso.capstone.global.config.AppConfig;
import com.knu.sosuso.capstone.global.exception.BusinessException;
import com.knu.sosuso.capstone.global.exception.error.CommonError;
import com.knu.sosuso.capstone.global.exception.error.SearchError;
import com.knu.sosuso.capstone.global.service.mapper.VideoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;

/**
 * YouTube 검색 서비스 (Fast Path - 점진적 로딩)
 * ✅ 처음 4개만 즉시 응답
 * ✅ 나머지는 백그라운드 처리
 * ✅ 스크롤 시 이미 준비된 데이터 응답
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
    private final CacheManager cacheManager;
    private final VideoProcessingService videoProcessingService;
    private final VideoRepository videoRepository;
    private final VideoMapper videoMapper;
    private final UserDataService userDataService;

    /**
     * 동영상 검색 (Fast Path)
     */
    @Transactional
    public SearchResultPageResponse searchVideos(String token, String query, String pageToken) {
        log.info("🔍 동영상 검색: query={}, pageToken={}", query, pageToken);
        return searchOptimizedFastPath(token, query, pageToken, VideoType.VIDEO);
    }

    /**
     * 쇼츠 검색 (Fast Path)
     */
    @Transactional
    public SearchResultPageResponse searchShorts(String token, String query, String pageToken) {
        log.info("🔍 쇼츠 검색: query={}, pageToken={}", query, pageToken);
        return searchOptimizedFastPath(token, query, pageToken, VideoType.SHORTS);
    }

    /**
     * ✅ Fast Path 검색 로직
     */
    private SearchResultPageResponse searchOptimizedFastPath(
            String token, String query, String pageToken, VideoType expectedType) {

        PageTokenInfo tokenInfo = parsePageToken(pageToken);

        log.info("📋 페이지 토큰 분석: batch={}, page={}, youtubeToken={}",
                tokenInfo.batchNum, tokenInfo.pageNum, tokenInfo.youtubeToken);

        // 1. 캐시에서 videoId 목록 조회
        String cacheKey = buildCacheKey(query, tokenInfo.youtubeToken);
        List<String> cachedVideoIds = getCachedVideoIds(cacheKey);

        if (cachedVideoIds == null) {
            // 캐시 미스 → Search API로 videoId만 가져오기
            log.info("🔴 캐시 미스: {} → Search API 호출", cacheKey);

            String searchResponse = callYouTubeSearchAPI(query, tokenInfo.youtubeToken, "any");
            SearchBatchResult batchResult = parseSearchResponse(searchResponse);

            if (batchResult.videoIds.isEmpty()) {
                return new SearchResultPageResponse(
                        Collections.emptyList(), null, 0, false
                );
            }

            cachedVideoIds = batchResult.videoIds;
            tokenInfo.nextYoutubeToken = batchResult.nextPageToken;

            // videoId 목록 캐시 저장
            cacheVideoIds(cacheKey, cachedVideoIds);

            log.info("✅ Search API 완료: {}개 videoId 받음", cachedVideoIds.size());

        } else {
            log.info("🟢 캐시 히트: {} → {} 개 videoId", cacheKey, cachedVideoIds.size());
        }

        // 2. 현재 페이지에 필요한 videoId 계산
        int pageSize = appConfig.getSearchPageSize();
        int startIdx = tokenInfo.pageNum * pageSize;
        int endIdx = Math.min(startIdx + pageSize, cachedVideoIds.size());

        if (startIdx >= cachedVideoIds.size()) {
            // 다음 배치 필요
            if (tokenInfo.nextYoutubeToken != null) {
                String nextPageToken = String.format("B%dP0Y%s",
                        tokenInfo.batchNum + 1,
                        tokenInfo.nextYoutubeToken);
                return searchOptimizedFastPath(token, query, nextPageToken, expectedType);
            }
            return new SearchResultPageResponse(
                    Collections.emptyList(), null, 0, false
            );
        }

        // 3. 현재 페이지 videoId 추출
        List<String> currentPageVideoIds = cachedVideoIds.subList(startIdx, endIdx);

        log.info("📄 페이지 범위: {}~{}/{}, 필요한 videoId: {}개",
                startIdx, endIdx, cachedVideoIds.size(), currentPageVideoIds.size());

        // 4. DB에서 이미 처리된 영상 조회
        List<Video> existingVideos = videoRepository.findAllByApiVideoIdIn(currentPageVideoIds);
        Set<String> existingVideoIds = existingVideos.stream()
                .map(Video::getApiVideoId)
                .collect(Collectors.toSet());

        // 5. 미처리 영상 필터링
        List<String> unprocessedVideoIds = currentPageVideoIds.stream()
                .filter(id -> !existingVideoIds.contains(id))
                .collect(Collectors.toList());

        // 6-1. ✅ 미처리 영상이 있으면 즉시 처리 (expectedType 전달)
        if (!unprocessedVideoIds.isEmpty()) {
            log.info("🔧 미처리 영상 {}개 즉시 처리", unprocessedVideoIds.size());
            processVideosSync(token, unprocessedVideoIds, expectedType);
        }

        // 6-2. ✅ 백그라운드로 나머지 처리 (expectedType 전달)
        if (tokenInfo.pageNum == 0 && endIdx < cachedVideoIds.size()) {
            List<String> remainingVideoIds = cachedVideoIds.subList(endIdx, cachedVideoIds.size());
            List<String> unprocessedRemaining = remainingVideoIds.stream()
                    .filter(id -> !existingVideoIds.contains(id))
                    .collect(Collectors.toList());

            if (!unprocessedRemaining.isEmpty()) {
                log.info("🔄 백그라운드 처리 시작: {}개 영상", unprocessedRemaining.size());
                processVideosAsync(token, unprocessedRemaining, expectedType);
            }
        }

        // 7. DB에서 최종 결과 조회 (타입 필터링 포함)
        List<Video> videos = videoRepository.findAllByApiVideoIdIn(currentPageVideoIds);

        // 8. ✅ VideoMapper 사용 + 타입 필터링
        List<VideoSummaryResponse> results = videos.stream()
                .filter(v -> {
                    boolean matches = v.getVideoType() == expectedType;
                    if (!matches) {
                        log.debug("타입 불일치로 제외: apiVideoId={}, expected={}, actual={}",
                                v.getApiVideoId(), expectedType, v.getVideoType());
                    }
                    return matches;
                })
                .map(v -> {
                    // 스크랩 ID 조회
                    Long scrapId = null;
                    try {
                        scrapId = userDataService.getUserScrapId(token, v.getApiVideoId());
                    } catch (Exception e) {
                        // 비로그인 사용자 또는 스크랩 없음
                    }

                    return videoMapper.toSummaryResponse(v, scrapId);
                })
                .collect(Collectors.toList());

        log.info("📊 타입 필터링: 전체 {}개 → {} {}개",
                videos.size(), expectedType, results.size());

        // ✅ 빈 결과 경고
        if (results.isEmpty() && !videos.isEmpty()) {
            log.warn("⚠️ 타입 필터링으로 모든 결과 제외: query={}, expectedType={}, 조회된 영상 타입:",
                    query, expectedType);
            for (Video v : videos) {
                log.warn("  - apiVideoId={}, actualType={}", v.getApiVideoId(), v.getVideoType());
            }
        }

        // 9. 페이징 응답 생성
        return createPagedResponse(results, tokenInfo, cachedVideoIds.size());
    }

    /**
     * ✅ 동기 처리 (즉시 필요한 영상) - expectedType 추가
     */
    private void processVideosSync(String token, List<String> videoIds, VideoType expectedType) {
        log.info("⚡ 동기 처리: {}개, expectedType={}", videoIds.size(), expectedType);

        List<VideoData> videoDataList = fetchVideosInBatch(videoIds);

        for (VideoData videoData : videoDataList) {
            try {
                VideoSummaryResponse response = videoProcessingService.processAndGetSummary(
                        token, videoData.apiVideoId, videoData.thumbnails, expectedType);

                if (response == null) {
                    log.debug("타입 불일치로 처리 스킵: apiVideoId={}, expectedType={}",
                            videoData.apiVideoId, expectedType);
                }
            } catch (Exception e) {
                log.warn("영상 처리 실패: apiVideoId={}, error={}", videoData.apiVideoId, e.getMessage());
            }
        }
    }

    /**
     * ✅ 비동기 처리 (백그라운드) - expectedType 추가
     */
    @Async("videoProcessingExecutor")
    public void processVideosAsync(String token, List<String> videoIds, VideoType expectedType) {
        log.info("🔄 비동기 처리 시작: {}개, expectedType={}", videoIds.size(), expectedType);

        // 20개씩 나눠서 처리 (Videos API 제한 고려)
        int batchSize = appConfig.getSearchBatchSize();
        for (int i = 0; i < videoIds.size(); i += batchSize) {
            int end = Math.min(i + batchSize, videoIds.size());
            List<String> batch = videoIds.subList(i, end);

            try {
                List<VideoData> videoDataList = fetchVideosInBatch(batch);

                for (VideoData videoData : videoDataList) {
                    try {
                        VideoSummaryResponse response = videoProcessingService.processAndGetSummary(
                                token, videoData.apiVideoId, videoData.thumbnails, expectedType);

                        if (response == null) {
                            log.debug("타입 불일치로 처리 스킵: apiVideoId={}, expectedType={}",
                                    videoData.apiVideoId, expectedType);
                        }
                    } catch (Exception e) {
                        log.error("백그라운드 영상 처리 실패: apiVideoId={}, error={}",
                                videoData.apiVideoId, e.getMessage());
                    }
                }

                log.info("✅ 백그라운드 배치 완료: {}/{}", end, videoIds.size());

            } catch (Exception e) {
                log.error("백그라운드 배치 오류: batch={}/{}, error={}",
                        i / batchSize + 1,
                        (videoIds.size() + batchSize - 1) / batchSize,
                        e.getMessage(), e);
            }
        }

        log.info("🎉 비동기 처리 완료: {}개", videoIds.size());
    }

    /**
     * 캐시 키 생성 (videoId 목록용)
     */
    private String buildCacheKey(String query, String youtubeToken) {
        String token = youtubeToken != null ? youtubeToken : "null";
        return String.format("%s:ids:batch%s", query, token);
    }

    /**
     * 캐시에서 videoId 목록 조회
     */
    @SuppressWarnings("unchecked")
    private List<String> getCachedVideoIds(String cacheKey) {
        Cache cache = cacheManager.getCache("searchResults");
        if (cache == null) return null;

        Cache.ValueWrapper wrapper = cache.get(cacheKey);
        return wrapper != null ? (List<String>) wrapper.get() : null;
    }

    /**
     * 캐시에 videoId 목록 저장
     */
    private void cacheVideoIds(String cacheKey, List<String> videoIds) {
        Cache cache = cacheManager.getCache("searchResults");
        if (cache != null) {
            cache.put(cacheKey, videoIds);
            log.info("💾 캐시 저장: key={}, videoIds={}", cacheKey, videoIds.size());
        }
    }

    /**
     * ✅ YouTube Search API 호출 - 에러 처리 강화
     */
    private String callYouTubeSearchAPI(String query, String pageToken, String videoDuration) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(YOUTUBE_SEARCH_API_URL)
                    .queryParam("part", "snippet")
                    .queryParam("type", "video")
                    .queryParam("q", query)
                    .queryParam("maxResults", appConfig.getSearchBatchSize())
                    .queryParam("relevanceLanguage", "ko")
                    .queryParam("key", apiConfig.getKey());

            if (videoDuration != null && !videoDuration.isEmpty()) {
                builder.queryParam("videoDuration", videoDuration);
            }

            if (pageToken != null && !pageToken.isEmpty()) {
                builder.queryParam("pageToken", pageToken);
            }

            String url = builder.build(false).toUriString();
            log.debug("YouTube Search API 호출: query={}, pageToken={}", query, pageToken);

            return restTemplate.getForObject(url, String.class);

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.error("YouTube API 접근 거부: 할당량 초과 또는 API 키 문제");
                throw new BusinessException(SearchError.YOUTUBE_API_ACCESS_DENIED);
            } else if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                log.error("잘못된 YouTube API 요청: query={}, pageToken={}", query, pageToken);
                throw new BusinessException(SearchError.INVALID_PAGE_TOKEN);
            }
            log.error("YouTube API 클라이언트 에러: status={}", e.getStatusCode());
            throw new BusinessException(SearchError.YOUTUBE_API_ERROR);

        } catch (HttpServerErrorException e) {
            log.error("YouTube API 서버 에러: status={}", e.getStatusCode());
            throw new BusinessException(SearchError.YOUTUBE_API_ERROR);

        } catch (ResourceAccessException e) {
            log.error("YouTube API 연결 실패: {}", e.getMessage());
            throw new BusinessException(SearchError.YOUTUBE_API_ERROR);

        } catch (Exception e) {
            log.error("YouTube API 호출 중 예상치 못한 에러", e);
            throw new BusinessException(CommonError.YOUTUBE_API_ERROR);
        }
    }

    /**
     * ✅ Videos API 배치 호출 - 에러 처리 강화
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

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.error("YouTube API 접근 거부");
                throw new BusinessException(SearchError.YOUTUBE_API_ACCESS_DENIED);
            }
            log.error("Videos API 클라이언트 에러: status={}", e.getStatusCode());
            throw new BusinessException(SearchError.YOUTUBE_API_ERROR);

        } catch (HttpServerErrorException e) {
            log.error("Videos API 서버 에러: status={}", e.getStatusCode());
            throw new BusinessException(SearchError.YOUTUBE_API_ERROR);

        } catch (ResourceAccessException e) {
            log.error("Videos API 연결 실패: {}", e.getMessage());
            throw new BusinessException(SearchError.YOUTUBE_API_ERROR);

        } catch (Exception e) {
            log.error("Videos API 호출 중 예상치 못한 에러", e);
            throw new BusinessException(CommonError.YOUTUBE_API_ERROR);
        }
    }

    /**
     * ✅ Videos API 응답 파싱 - 에러 처리 개선
     */
    private List<VideoData> parseVideosResponse(String response) {
        try {
            if (response == null || response.trim().isEmpty()) {
                log.warn("Videos API 응답이 비어있음");
                throw new BusinessException(CommonError.DATA_PARSING_ERROR);
            }

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

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Videos API 응답 파싱 실패", e);
            throw new BusinessException(CommonError.DATA_PARSING_ERROR);
        }
    }

    /**
     * ✅ Search API 응답 파싱 - 에러 처리 개선
     */
    private SearchBatchResult parseSearchResponse(String response) {
        try {
            if (response == null || response.trim().isEmpty()) {
                log.warn("Search API 응답이 비어있음");
                throw new BusinessException(CommonError.DATA_PARSING_ERROR);
            }

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

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Search API 응답 파싱 실패", e);
            throw new BusinessException(CommonError.DATA_PARSING_ERROR);
        }
    }

    /**
     * 페이지 토큰 파싱
     */
    private PageTokenInfo parsePageToken(String pageToken) {
        PageTokenInfo info = new PageTokenInfo();

        if (pageToken == null || pageToken.isEmpty()) {
            return info;
        }

        try {
            int bIndex = pageToken.indexOf('B');
            int pIndex = pageToken.indexOf('P');
            int yIndex = pageToken.indexOf('Y');

            info.batchNum = Integer.parseInt(pageToken.substring(bIndex + 1, pIndex));
            info.pageNum = Integer.parseInt(pageToken.substring(pIndex + 1, yIndex));

            String token = pageToken.substring(yIndex + 1);
            info.youtubeToken = "null".equals(token) ? null : token;

        } catch (Exception e) {
            log.warn("페이지 토큰 파싱 실패: {}", pageToken);
        }

        return info;
    }

    /**
     * 페이징 응답 생성
     */
    private SearchResultPageResponse createPagedResponse(
            List<VideoSummaryResponse> results,
            PageTokenInfo tokenInfo,
            int totalCachedCount) {

        int pageSize = appConfig.getSearchPageSize();
        int nextPageStart = (tokenInfo.pageNum + 1) * pageSize;

        String nextPageToken = null;
        boolean hasMore = false;

        if (nextPageStart < totalCachedCount) {
            // 같은 배치 내 다음 페이지
            nextPageToken = String.format("B%dP%dY%s",
                    tokenInfo.batchNum,
                    tokenInfo.pageNum + 1,
                    tokenInfo.youtubeToken != null ? tokenInfo.youtubeToken : "null");
            hasMore = true;

        } else if (tokenInfo.nextYoutubeToken != null) {
            // 다음 배치
            nextPageToken = String.format("B%dP0Y%s",
                    tokenInfo.batchNum + 1,
                    tokenInfo.nextYoutubeToken);
            hasMore = true;
        }

        return new SearchResultPageResponse(results, nextPageToken, results.size(), hasMore);
    }

    // ==================== 내부 클래스 ====================

    private static class PageTokenInfo {
        int batchNum = 0;
        int pageNum = 0;
        String youtubeToken = null;
        String nextYoutubeToken = null;
    }

    private static class SearchBatchResult {
        final List<String> videoIds;
        final String nextPageToken;

        SearchBatchResult(List<String> videoIds, String nextPageToken) {
            this.videoIds = videoIds;
            this.nextPageToken = nextPageToken;
        }
    }

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
}