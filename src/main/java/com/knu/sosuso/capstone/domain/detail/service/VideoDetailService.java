package com.knu.sosuso.capstone.domain.detail.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.sosuso.capstone.domain.comment.dto.CommentDto;
import com.knu.sosuso.capstone.domain.comment.entity.Comment;
import com.knu.sosuso.capstone.domain.comment.entity.value.SentimentType;
import com.knu.sosuso.capstone.domain.comment.repository.CommentRepository;
import com.knu.sosuso.capstone.domain.detail.dto.*;
import com.knu.sosuso.capstone.domain.video.entity.Video;
import com.knu.sosuso.capstone.domain.video.entity.VideoStatusHelper;
import com.knu.sosuso.capstone.domain.video.entity.value.AIAnalysisStatus;
import com.knu.sosuso.capstone.domain.video.repository.VideoRepository;
import com.knu.sosuso.capstone.domain.video.service.UserDataService;
import com.knu.sosuso.capstone.domain.video.service.VideoProcessingService;
import com.knu.sosuso.capstone.global.config.AppConfig;
import com.knu.sosuso.capstone.global.exception.BusinessException;
import com.knu.sosuso.capstone.global.exception.error.CommonError;
import com.knu.sosuso.capstone.global.exception.error.VideoError;
import com.knu.sosuso.capstone.global.service.mapper.ResponseMappingService;
import com.knu.sosuso.capstone.global.service.mapper.VideoMapper;
import com.knu.sosuso.capstone.global.service.mapper.CommentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 영상 상세 조회 서비스
 * API 분리로 영상 기본/분석/댓글/AI 정보를 독립적으로 제공
 * AIAnalysisStatus와 @Retryable 적용
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class VideoDetailService {

    private final VideoRepository videoRepository;
    private final CommentRepository commentRepository;
    private final UserDataService userDataService;
    private final VideoProcessingService videoProcessingService;
    private final ResponseMappingService responseMappingService;
    private final ObjectMapper objectMapper;
    private final VideoMapper videoMapper;
    private final CommentMapper commentMapper;
    private final AppConfig appConfig;

    private static final int MAX_AI_RETRY_COUNT = 3;

    /**
     * 영상 기본 정보 조회
     * 새 영상이면 YouTube API에서 수집하고 백그라운드 AI 스케줄
     * 기존 영상이면 DB에서 조회하고, AI 미완료 시 재시도
     * 캐싱: 30분 TTL
     */
    @Cacheable(value = "videoDetail", key = "'basic-' + #apiVideoId", unless = "#result == null")
    @Transactional
    public VideoBasicResponse getVideoBasic(String token, String apiVideoId) {
        log.info("영상 기본 정보 조회: apiVideoId={}", apiVideoId);

        // DB에서 조회 시도
        Video video = videoRepository.findByApiVideoId(apiVideoId).orElse(null);

        if (video == null) {
            // DB에 없으면 YouTube API에서 수집 (AI 없이)
            log.info("DB에 없는 영상, YouTube API에서 수집: apiVideoId={}", apiVideoId);
            DetailPageResponse fullResponse = videoProcessingService.processVideoToSearchResult(
                    token, apiVideoId, false);

            // 새로 저장된 영상이면 백그라운드 AI 스케줄링
            Video savedVideo = videoRepository.findByApiVideoId(apiVideoId).orElse(null);
            if (savedVideo != null && VideoStatusHelper.needsAIAnalysis(savedVideo)) {
                log.info("백그라운드 AI 분석 스케줄: apiVideoId={}", apiVideoId);
                videoProcessingService.scheduleBackgroundAIProcessingWithRetry(
                        savedVideo.getId(), apiVideoId);
            }

            return new VideoBasicResponse(
                    fullResponse.video(),
                    fullResponse.channel()
            );
        }

        // 기존 영상인데 AI 미완료면 백그라운드 AI 재시도
        if (VideoStatusHelper.needsAIAnalysis(video) &&
                VideoStatusHelper.canRetry(video, MAX_AI_RETRY_COUNT)) {
            log.info("기존 영상 백그라운드 AI 재시도: apiVideoId={}, status={}, retryCount={}",
                    apiVideoId, video.getAiAnalysisStatus(), video.getAiRetryCount());
            videoProcessingService.scheduleBackgroundAIProcessingWithRetry(
                    video.getId(), apiVideoId);
        }

        // DB에서 기본 정보 반환
        Long scrapId = userDataService.getUserScrapId(token, apiVideoId);
        Long favoriteChannelId = userDataService.getUserFavoriteChannelId(token, video.getChannelId());

        DetailVideoDto videoDto = videoMapper.toDetailVideoDto(video, scrapId);
        DetailChannelDto channelDto = videoMapper.toDetailChannelDto(video, favoriteChannelId);

        log.info("영상 기본 정보 조회 완료: apiVideoId={}, aiStatus={}",
                apiVideoId, video.getAiAnalysisStatus());
        return new VideoBasicResponse(videoDto, channelDto);
    }

    /**
     * 영상 분석 정보 조회 (백엔드 분석 + TOP 5 댓글 + 감정 흐름)
     * 캐싱: 30분 TTL
     */
    @Cacheable(value = "videoDetail", key = "'analysis-' + #apiVideoId", unless = "#result == null")
    @Transactional(readOnly = true)
    public VideoAnalysisResponse getVideoAnalysis(String apiVideoId) {
        log.info("영상 분석 정보 조회: apiVideoId={}", apiVideoId);

        Video video = videoRepository.findByApiVideoId(apiVideoId)
                .orElseThrow(() -> new BusinessException(VideoError.VIDEO_NOT_FOUND));

        // 1. 댓글 히스토그램
        Map<Integer, Integer> commentHistogramData = parseJsonToMap(
                video.getCommentHistogram(), Integer.class, Integer.class);
        List<DetailAnalysisDto.CommentHistogram> commentHistogram =
                commentHistogramData.entrySet().stream()
                        .map(e -> new DetailAnalysisDto.CommentHistogram(String.valueOf(e.getKey()), e.getValue()))
                        .collect(Collectors.toList());

        // 2. 인기 타임스탬프
        Map<String, Integer> popularTimestampsData = parseJsonToMap(
                video.getPopularTimestamps(), String.class, Integer.class);
        List<DetailAnalysisDto.PopularTimestamp> popularTimestamps =
                popularTimestampsData.entrySet().stream()
                        .map(e -> new DetailAnalysisDto.PopularTimestamp(e.getKey(), e.getValue()))
                        .collect(Collectors.toList());

        // 3. TOP 5 댓글 (세부 감정 포함)
        List<Comment> topCommentsData = commentRepository
                .findByVideoIdOrderByLikeCountDesc(video.getId())
                .stream()
                .limit(appConfig.getTopCommentsCount())
                .collect(Collectors.toList());

        List<CommentDto> topComments = commentMapper.toTopCommentDtoList(topCommentsData);

        // 4. 감정 흐름 분석
        List<DetailAnalysisDto.SentimentFlow> sentimentFlow = calculateSentimentFlow(video);

        log.info("영상 분석 정보 조회 완료: apiVideoId={}, TOP 댓글 수={}, 감정 흐름 데이터={}개",
                apiVideoId, topComments.size(), sentimentFlow.size());

        return new VideoAnalysisResponse(commentHistogram, popularTimestamps, topComments, sentimentFlow);
    }

    /**
     * 전체 댓글 조회 (세부 감정 포함)
     */
    @Transactional(readOnly = true)
    public List<CommentDto> getVideoComments(String apiVideoId) {
        log.info("전체 댓글 조회: apiVideoId={}", apiVideoId);

        Video video = videoRepository.findByApiVideoId(apiVideoId)
                .orElseThrow(() -> new BusinessException(VideoError.VIDEO_NOT_FOUND));

        List<Comment> comments = commentRepository.findByVideoIdOrderByIdAsc(video.getId());
        List<CommentDto> commentDtos = commentMapper.toDtoList(comments);

        log.info("전체 댓글 조회 완료: apiVideoId={}, 댓글 수={}", apiVideoId, commentDtos.size());
        return commentDtos;
    }

    /**
     * AI 분석 결과 조회
     * 캐싱: 30분 TTL
     */
    @Cacheable(value = "videoDetail", key = "'ai-' + #apiVideoId", unless = "#result == null")
    @Transactional(readOnly = true)
    public AIAnalysisResponse getAIAnalysis(String apiVideoId) {
        log.info("AI 분석 결과 조회: apiVideoId={}", apiVideoId);

        Video video = videoRepository.findByApiVideoId(apiVideoId)
                .orElseThrow(() -> new BusinessException(VideoError.VIDEO_NOT_FOUND));

        // AIAnalysisStatus로 확인
        if (video.getAiAnalysisStatus() != AIAnalysisStatus.COMPLETED &&
                video.getAiAnalysisStatus() != AIAnalysisStatus.PARTIAL) {
            log.info("AI 분석 미완료: apiVideoId={}, status={}",
                    apiVideoId, video.getAiAnalysisStatus());

            // 재시도 가능한 상태인지 확인하고 백그라운드 처리 스케줄
            if (VideoStatusHelper.canRetry(video, MAX_AI_RETRY_COUNT)) {
                log.info("AI 재시도 스케줄링: apiVideoId={}", apiVideoId);
                videoProcessingService.scheduleBackgroundAIProcessingWithRetry(
                        video.getId(), apiVideoId);
            }

            return new AIAnalysisResponse(null, null, List.of(), null, List.of());
        }

        // AI 분석 완료
        Map<String, Double> languageRatio = parseJsonToMap(
                video.getLanguageDistribution(), String.class, Double.class);
        List<DetailAnalysisDto.LanguageDistribution> languageDistribution =
                languageRatio.entrySet().stream()
                        .map(e -> new DetailAnalysisDto.LanguageDistribution(e.getKey(), e.getValue()))
                        .collect(Collectors.toList());

        Map<String, Double> sentimentRatio = parseJsonToMap(
                video.getSentimentDistribution(), String.class, Double.class);
        DetailAnalysisDto.SentimentDistribution sentimentDistribution =
                new DetailAnalysisDto.SentimentDistribution(
                        sentimentRatio.getOrDefault("positive", 0.0),
                        sentimentRatio.getOrDefault("negative", 0.0),
                        sentimentRatio.getOrDefault("other", 0.0)
                );

        List<String> keywords = parseJsonToList(video.getKeywords(), String.class);

        log.info("AI 분석 결과 조회 완료: apiVideoId={}, status={}",
                apiVideoId, video.getAiAnalysisStatus());
        return new AIAnalysisResponse(
                video.getSummation(),
                video.isWarning(),
                languageDistribution,
                sentimentDistribution,
                keywords
        );
    }

    /**
     * Deprecated 통합 API (캐싱 적용)
     * 프론트엔드 마이그레이션 완료 후 제거 예정
     */
    @Deprecated
    @Cacheable(
            value = "videoDetail",
            key = "#apiVideoId",
            unless = "#result == null"
    )
    @Transactional(readOnly = true)
    public DetailPageResponse getVideoDetail(String token, String apiVideoId) {
        log.info("영상 상세 조회 (캐시 미스, Deprecated): apiVideoId={}", apiVideoId);

        Video video = videoRepository.findByApiVideoId(apiVideoId).orElse(null);

        if (video != null) {
            if (video.isDeleted()) {
                throw new BusinessException(VideoError.VIDEO_DELETED);
            }

            // DB에서 조회 가능하면 캐시 히트 확률 높음
            return responseMappingService.mapFromDbToSearchResult(token, video);
        }

        // 캐시 미스이고 DB에도 없으면 YouTube API 호출
        return videoProcessingService.processVideoToSearchResult(token, apiVideoId, false);
    }

    /**
     * 영상 상세 캐시 무효화
     * AI 분석 완료 후 VideoProcessingService에서 호출
     */
    @CacheEvict(value = "videoDetail", key = "#apiVideoId")
    public void evictVideoCache(String apiVideoId) {
        log.info("영상 상세 캐시 무효화: apiVideoId={}", apiVideoId);
    }

    // ==================== Sentiment Flow Calculation ====================

    /**
     * 감정 흐름 분석 계산
     * 시간 순서대로 댓글의 감정 비율 변화 추이
     */
    private List<DetailAnalysisDto.SentimentFlow> calculateSentimentFlow(Video video) {
        try {
            // 1. 댓글이 없거나 AI 미완료면 빈 리스트 반환
            if (video.getCommentCount() == null ||
                    Integer.parseInt(video.getCommentCount()) == 0 ||
                    !isAIAnalysisCompleted(video)) {
                log.debug("감정 흐름 계산 불가: 댓글 없음 또는 AI 미완료, videoId={}, aiStatus={}",
                        video.getId(), video.getAiAnalysisStatus());
                return new ArrayList<>();
            }

            // 2. 감정 분석이 있는 댓글만 조회
            List<Comment> comments = commentRepository.findByVideoIdAndSentimentTypeIsNotNull(video.getId());

            if (comments.isEmpty()) {
                log.debug("감정 분석된 댓글 없음, 빈 감정 흐름 반환: videoId={}", video.getId());
                return new ArrayList<>();
            }

            // 3. 날짜별로 댓글 그룹화
            Map<LocalDate, List<Comment>> commentsByDate = comments.stream()
                    .collect(Collectors.groupingBy(comment ->
                            parseCommentDate(comment.getWrittenAt())));

            if (commentsByDate.isEmpty()) {
                log.debug("날짜별 그룹화 실패, 빈 감정 흐름 반환: videoId={}", video.getId());
                return new ArrayList<>();
            }

            // 4. 날짜 순으로 정렬
            List<LocalDate> sortedDates = commentsByDate.keySet().stream()
                    .sorted()
                    .collect(Collectors.toList());

            // 5. 각 날짜의 감정 분포 계산
            List<DailySentimentData> allDailyData = sortedDates.stream()
                    .map(date -> calculateDailySentiment(date, commentsByDate.get(date)))
                    .collect(Collectors.toList());

            // 6. 최대 N개로 샘플링
            List<DetailAnalysisDto.SentimentFlow> sampledData =
                    sampleSentimentData(allDailyData);

            log.info("감정 흐름 계산 완료: videoId={}, 전체 {}일 -> 샘플링 {}개",
                    video.getId(), allDailyData.size(), sampledData.size());

            return sampledData;

        } catch (Exception e) {
            log.warn("감정 흐름 계산 중 오류 발생, 빈 리스트 반환: videoId={}, error={}",
                    video.getId(), e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 하루치 감정 데이터 계산
     */
    private DailySentimentData calculateDailySentiment(LocalDate date, List<Comment> dayComments) {
        long total = dayComments.size();
        long positive = dayComments.stream()
                .filter(c -> c.getSentimentType() == SentimentType.POSITIVE)
                .count();
        long negative = dayComments.stream()
                .filter(c -> c.getSentimentType() == SentimentType.NEGATIVE)
                .count();
        long other = total - positive - negative;

        return new DailySentimentData(
                date,
                total > 0 ? positive / (double) total : 0.0,
                total > 0 ? negative / (double) total : 0.0,
                total > 0 ? other / (double) total : 0.0
        );
    }

    /**
     * 감정 데이터를 최대 N개로 샘플링
     * 전체 기간을 균등하게 나누어 대표값 선택
     */
    private List<DetailAnalysisDto.SentimentFlow> sampleSentimentData(
            List<DailySentimentData> allData) {

        int totalDays = allData.size();
        int maxPoints = appConfig.getSentimentFlowMaxDataPoints();

        // maxPoints 이하면 전체 반환
        if (totalDays <= maxPoints) {
            return allData.stream()
                    .map(this::convertToSentimentFlow)
                    .collect(Collectors.toList());
        }

        // maxPoints를 초과하면 균등 간격으로 샘플링
        List<DetailAnalysisDto.SentimentFlow> result = new ArrayList<>();

        // 첫 번째는 항상 포함
        result.add(convertToSentimentFlow(allData.get(0)));

        // 중간 포인트들을 균등 간격으로 선택
        double step = (totalDays - 1) / (double) (maxPoints - 1);

        for (int i = 1; i < maxPoints - 1; i++) {
            int index = (int) Math.round(step * i);
            result.add(convertToSentimentFlow(allData.get(index)));
        }

        // 마지막은 항상 포함
        result.add(convertToSentimentFlow(allData.get(totalDays - 1)));

        return result;
    }

    /**
     * 내부 데이터를 응답 DTO로 변환
     */
    private DetailAnalysisDto.SentimentFlow convertToSentimentFlow(DailySentimentData data) {
        return new DetailAnalysisDto.SentimentFlow(
                data.date().toString(),
                data.positive(),
                data.negative(),
                data.other()
        );
    }

    /**
     * 날짜 파싱 헬퍼 메서드
     */
    private LocalDate parseCommentDate(String writtenAt) {
        try {
            if (writtenAt == null || writtenAt.length() < 10) {
                return LocalDate.now();
            }
            return LocalDate.parse(writtenAt.substring(0, 10));
        } catch (Exception e) {
            log.warn("댓글 날짜 파싱 실패: {}", writtenAt);
            return LocalDate.now();
        }
    }

    /**
     * 내부 계산용 데이터 클래스
     */
    private record DailySentimentData(
            LocalDate date,
            Double positive,
            Double negative,
            Double other
    ) {
    }

    // ==================== Helper Methods ====================

    /**
     * AI 분석 완료 여부 확인 (AIAnalysisStatus 기반)
     */
    private boolean isAIAnalysisCompleted(Video video) {
        // AIAnalysisStatus 우선 확인
        if (video.getAiAnalysisStatus() != null) {
            return video.getAiAnalysisStatus() == AIAnalysisStatus.COMPLETED ||
                    video.getAiAnalysisStatus() == AIAnalysisStatus.PARTIAL;
        }

        // Fallback: 기존 방식 (하위 호환성)
        return video.getSummation() != null &&
                video.getLanguageDistribution() != null &&
                video.getSentimentDistribution() != null &&
                video.getKeywords() != null;
    }

    /**
     * AI 재시도 가능 여부 확인 (VideoStatusHelper 활용)
     * @deprecated Use VideoStatusHelper.canRetry() directly
     */
    @Deprecated
    private boolean shouldRetryAI(Video video) {
        return VideoStatusHelper.canRetry(video, MAX_AI_RETRY_COUNT);
    }

    private <K, V> Map<K, V> parseJsonToMap(String json, Class<K> keyClass, Class<V> valueClass) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructMapType(Map.class, keyClass, valueClass));
        } catch (Exception e) {
            log.warn("JSON 파싱 실패: {}", e.getMessage());
            throw new BusinessException(CommonError.DATA_PARSING_ERROR);
        }
    }

    private <T> List<T> parseJsonToList(String json, Class<T> elementClass) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, elementClass));
        } catch (Exception e) {
            log.warn("JSON List 파싱 실패: {}", e.getMessage());
            throw new BusinessException(CommonError.DATA_PARSING_ERROR);
        }
    }

    /**
     * 비디오 캐시 무효화
     * 비디오 업데이트 시 호출
     */
    @CacheEvict(value = "videoDetail", allEntries = false,
            key = "'basic-' + #apiVideoId")
    public void evictVideoBasicCache(String apiVideoId) {
        log.info("비디오 기본 정보 캐시 삭제: apiVideoId={}", apiVideoId);
    }

    @CacheEvict(value = "videoDetail", allEntries = false,
            key = "'analysis-' + #apiVideoId")
    public void evictVideoAnalysisCache(String apiVideoId) {
        log.info("비디오 분석 정보 캐시 삭제: apiVideoId={}", apiVideoId);
    }

    @CacheEvict(value = "videoDetail", allEntries = false,
            key = "'ai-' + #apiVideoId")
    public void evictVideoAICache(String apiVideoId) {
        log.info("비디오 AI 정보 캐시 삭제: apiVideoId={}", apiVideoId);
    }

    /**
     * 특정 비디오의 모든 캐시 삭제
     */
    public void evictAllVideoCache(String apiVideoId) {
        evictVideoBasicCache(apiVideoId);
        evictVideoAnalysisCache(apiVideoId);
        evictVideoAICache(apiVideoId);
        log.info("비디오 전체 캐시 삭제 완료: apiVideoId={}", apiVideoId);
    }

    private Long parseLong(String value) {
        try {
            return value != null && !value.isEmpty() ? Long.parseLong(value) : 0L;
        } catch (NumberFormatException e) {
            log.warn("Long 변환 실패: {}", value);
            return 0L;
        }
    }

    private Integer parseInt(String value) {
        try {
            return value != null && !value.isEmpty() ? Integer.parseInt(value) : 0;
        } catch (NumberFormatException e) {
            log.warn("Integer 변환 실패: {}", value);
            return 0;
        }
    }
}