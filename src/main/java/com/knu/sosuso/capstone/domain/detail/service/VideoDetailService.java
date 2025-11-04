package com.knu.sosuso.capstone.domain.detail.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.sosuso.capstone.domain.comment.repository.CommentRepository;
import com.knu.sosuso.capstone.domain.detail.dto.*;
import com.knu.sosuso.capstone.domain.video.entity.Video;
import com.knu.sosuso.capstone.domain.video.repository.VideoRepository;
import com.knu.sosuso.capstone.domain.video.service.UserDataService;
import com.knu.sosuso.capstone.domain.video.service.VideoProcessingService;
import com.knu.sosuso.capstone.global.exception.BusinessException;
import com.knu.sosuso.capstone.global.exception.error.CommonError;
import com.knu.sosuso.capstone.global.exception.error.VideoError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class VideoDetailService {

    private final VideoRepository videoRepository;
    private final CommentRepository commentRepository;
    private final UserDataService userDataService;
    private final VideoProcessingService videoProcessingService;
    private final ObjectMapper objectMapper;

    /**
     * 영상 기본 정보 조회
     */
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

            return new VideoBasicResponse(
                    fullResponse.video(),
                    fullResponse.channel()
            );
        }

        // DB에서 기본 정보 반환
        Long scrapId = userDataService.getUserScrapId(token, apiVideoId);
        Long favoriteChannelId = userDataService.getUserFavoriteChannelId(token, video.getChannelId());

        DetailVideoDto videoDto = new DetailVideoDto(
                video.getApiVideoId(),
                video.getTitle(),
                video.getDescription(),
                video.getUploadedAt(),
                video.getThumbnailUrl(),
                parseLong(video.getViewCount()),
                parseLong(video.getLikeCount()),
                parseInt(video.getCommentCount()),
                scrapId
        );

        DetailChannelDto channelDto = new DetailChannelDto(
                video.getChannelId(),
                video.getChannelName(),
                video.getChannelThumbnailUrl(),
                parseLong(video.getSubscriberCount()),
                favoriteChannelId
        );

        log.info("영상 기본 정보 조회 완료: apiVideoId={}", apiVideoId);
        return new VideoBasicResponse(videoDto, channelDto);
    }

    /**
     * 영상 분석 정보 조회 (백엔드 분석 + TOP 5 댓글)
     */
    @Transactional(readOnly = true)
    public VideoAnalysisResponse getVideoAnalysis(String apiVideoId) {
        log.info("영상 분석 정보 조회: apiVideoId={}", apiVideoId);

        Video video = videoRepository.findByApiVideoId(apiVideoId)
                .orElseThrow(() -> new BusinessException(VideoError.VIDEO_NOT_FOUND));

        // 댓글 히스토그램
        Map<Integer, Integer> commentHistogramData = parseJsonToMap(
                video.getCommentHistogram(), Integer.class, Integer.class);
        List<DetailAnalysisDto.CommentHistogram> commentHistogram =
                commentHistogramData.entrySet().stream()
                        .map(e -> new DetailAnalysisDto.CommentHistogram(String.valueOf(e.getKey()), e.getValue()))
                        .collect(Collectors.toList());

        // 인기 타임스탬프
        Map<String, Integer> popularTimestampsData = parseJsonToMap(
                video.getPopularTimestamps(), String.class, Integer.class);
        List<DetailAnalysisDto.PopularTimestamp> popularTimestamps =
                popularTimestampsData.entrySet().stream()
                        .map(e -> new DetailAnalysisDto.PopularTimestamp(e.getKey(), e.getValue()))
                        .collect(Collectors.toList());

        // TOP 5 댓글
        List<DetailCommentDto> topComments = commentRepository
                .findByVideoIdOrderByLikeCountDesc(video.getId())
                .stream()
                .limit(5)
                .map(c -> new DetailCommentDto(
                        c.getApiCommentId(),
                        c.getWriter(),
                        c.getCommentContent(),
                        c.getLikeCount(),
                        c.getSentimentType() != null ? c.getSentimentType().name() : null,
                        c.getWrittenAt()
                ))
                .collect(Collectors.toList());

        log.info("영상 분석 정보 조회 완료: apiVideoId={}, TOP 댓글 수={}", apiVideoId, topComments.size());
        return new VideoAnalysisResponse(commentHistogram, popularTimestamps, topComments);
    }

    /**
     * 전체 댓글 조회
     */
    @Transactional(readOnly = true)
    public List<DetailCommentDto> getVideoComments(String apiVideoId) {
        log.info("전체 댓글 조회: apiVideoId={}", apiVideoId);

        Video video = videoRepository.findByApiVideoId(apiVideoId)
                .orElseThrow(() -> new BusinessException(VideoError.VIDEO_NOT_FOUND));

        List<DetailCommentDto> comments = commentRepository.findByVideoIdOrderByIdAsc(video.getId())
                .stream()
                .map(c -> new DetailCommentDto(
                        c.getApiCommentId(),
                        c.getWriter(),
                        c.getCommentContent(),
                        c.getLikeCount(),
                        c.getSentimentType() != null ? c.getSentimentType().name() : null,
                        c.getWrittenAt()
                ))
                .collect(Collectors.toList());

        log.info("전체 댓글 조회 완료: apiVideoId={}, 댓글 수={}", apiVideoId, comments.size());
        return comments;
    }

    /**
     * AI 분석 결과 조회
     */
    @Transactional(readOnly = true)
    public AIAnalysisResponse getAIAnalysis(String apiVideoId) {
        log.info("AI 분석 결과 조회: apiVideoId={}", apiVideoId);

        Video video = videoRepository.findByApiVideoId(apiVideoId)
                .orElseThrow(() -> new BusinessException(VideoError.VIDEO_NOT_FOUND));

        // AI 분석 완료 여부 체크
        boolean hasAIAnalysis = video.getSummation() != null &&
                video.getLanguageDistribution() != null &&
                video.getSentimentDistribution() != null &&
                video.getKeywords() != null;

        if (!hasAIAnalysis) {
            log.info("AI 분석이 아직 완료되지 않음: apiVideoId={}", apiVideoId);
            // 빈 응답 반환 (프론트에서 "분석 중" 표시)
            return new AIAnalysisResponse(
                    null,
                    false,
                    new ArrayList<>(),
                    new DetailAnalysisDto.SentimentDistribution(0.0, 0.0, 0.0),
                    new ArrayList<>()
            );
        }

        // 언어 분포
        Map<String, Double> languageRatio = parseJsonToMap(
                video.getLanguageDistribution(), String.class, Double.class);
        List<DetailAnalysisDto.LanguageDistribution> languageDistribution =
                languageRatio.entrySet().stream()
                        .map(e -> new DetailAnalysisDto.LanguageDistribution(e.getKey(), e.getValue()))
                        .collect(Collectors.toList());

        // 감정 분포
        Map<String, Double> sentimentRatio = parseJsonToMap(
                video.getSentimentDistribution(), String.class, Double.class);
        DetailAnalysisDto.SentimentDistribution sentimentDistribution =
                new DetailAnalysisDto.SentimentDistribution(
                        sentimentRatio.getOrDefault("positive", 0.0),
                        sentimentRatio.getOrDefault("negative", 0.0),
                        sentimentRatio.getOrDefault("other", 0.0)
                );

        // 키워드
        List<String> keywords = new ArrayList<>();
        try {
            keywords = objectMapper.readValue(video.getKeywords(), new TypeReference<>() {
            });
        } catch (Exception e) {
            log.warn("키워드 파싱 실패: {}", e.getMessage());
        }

        log.info("AI 분석 결과 조회 완료: apiVideoId={}", apiVideoId);
        return new AIAnalysisResponse(
                video.getSummation(),
                video.isWarning(),
                languageDistribution,
                sentimentDistribution,
                keywords
        );
    }

    /**
     * @deprecated 기존 통합 API
     */
    @Deprecated
    @Transactional
    public DetailPageResponse getVideoDetail(String token, String apiVideoId) {
        log.warn("Deprecated 메서드 호출: getVideoDetail() - 분리된 메서드 사용 권장");
        return videoProcessingService.processVideoToSearchResult(token, apiVideoId, true);
    }

    // ==================== Helper Methods ====================

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