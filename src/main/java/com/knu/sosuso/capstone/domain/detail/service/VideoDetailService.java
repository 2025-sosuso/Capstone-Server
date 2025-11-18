package com.knu.sosuso.capstone.domain.detail.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.sosuso.capstone.domain.comment.dto.CommentDto;
import com.knu.sosuso.capstone.domain.comment.entity.Comment;
import com.knu.sosuso.capstone.domain.comment.entity.value.DetailSentimentType;
import com.knu.sosuso.capstone.domain.comment.entity.value.SentimentType;
import com.knu.sosuso.capstone.domain.comment.repository.CommentRepository;
import com.knu.sosuso.capstone.domain.detail.dto.*;
import com.knu.sosuso.capstone.domain.scrap.entity.Scrap;
import com.knu.sosuso.capstone.domain.scrap.repository.ScrapRepository;
import com.knu.sosuso.capstone.domain.video.dto.response.VideoApiResponse;
import com.knu.sosuso.capstone.domain.video.entity.Video;
import com.knu.sosuso.capstone.domain.video.entity.VideoType;
import com.knu.sosuso.capstone.domain.video.repository.VideoRepository;
import com.knu.sosuso.capstone.domain.video.service.UserDataService;
import com.knu.sosuso.capstone.domain.video.service.VideoProcessingService;
import com.knu.sosuso.capstone.domain.video.service.VideoService;
import com.knu.sosuso.capstone.global.config.AppConfig;
import com.knu.sosuso.capstone.global.exception.BusinessException;
import com.knu.sosuso.capstone.global.exception.error.VideoError;
import com.knu.sosuso.capstone.global.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class VideoDetailService {

    private final VideoService videoService;
    private final VideoProcessingService videoProcessingService;
    private final VideoRepository videoRepository;
    private final CommentRepository commentRepository;
    private final ScrapRepository scrapRepository;
    private final JwtUtil jwtUtil;
    private final AppConfig appConfig;
    private final UserDataService userDataService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ========================================
    // 1. 영상 기본 정보 (영상 정보 + 댓글 수집)
    // ========================================

    /**
     * 영상 엔티티 조회 또는 처리 (사용자 무관 - 캐시 가능)
     * - 영상 메타데이터만 캐시
     */
    @Cacheable(value = "videoDetail", key = "'video-' + #apiVideoId")
    @Transactional
    public Video getOrProcessVideo(String apiVideoId) {
        log.info("📺 영상 엔티티 조회: apiVideoId={}", apiVideoId);

        // 1. DB에서 먼저 찾기
        Optional<Video> existingVideo = videoRepository.findByApiVideoId(apiVideoId);

        Video video;
        if (existingVideo.isPresent()) {
            // ✅ 기존 영상: 자체 처리
            video = existingVideo.get();

            if (shouldCheckDeletion(video)) {
                checkAndUpdateDeletionStatus(video);
            }

            if (shouldUpdateMetadata(video)) {
                updateMetadata(video);
            }

            log.info("✅ 기존 영상 조회: apiVideoId={}", apiVideoId);
        } else {
            log.info("🆕 새 영상 처리 시작: apiVideoId={}", apiVideoId);
            video = videoProcessingService.processNewVideo(apiVideoId, VideoType.VIDEO);
            log.info("✅ 새 영상 처리 완료: apiVideoId={}", apiVideoId);
        }

        return video;
    }

    /**
     * 영상 기본 정보 조회
     */
    @Cacheable(value = "videoDetail", key = "'basic-' + #apiVideoId", unless = "#result == null")
    @Transactional(readOnly = true)
    public VideoBasicResponse getVideoBasic(String token, String apiVideoId) {
        log.info("📺 영상 기본 정보 조회: apiVideoId={}", apiVideoId);

        // 캐시된 영상 정보 조회
        Video video = getOrProcessVideo(apiVideoId);

        return createBasicResponse(null, video);
    }

    /**
     * 사용자별 영상 상태 조회 (캐시 사용 안 함)
     * - 스크랩 여부
     * - 관심 채널 여부
     */
    @Transactional(readOnly = true)
    public UserVideoStateResponse getUserVideoState(String token, String apiVideoId) {
        log.info("👤 사용자 영상 상태 조회: apiVideoId={}", apiVideoId);

        Long scrapId = null;
        Long favoriteChannelId = null;

        if (token != null && jwtUtil.isValidToken(token)) {
            Long userId = jwtUtil.getUserId(token);

            // 비디오 조회 (캐시 활용)
            Video video = getOrProcessVideo(apiVideoId);

            // 스크랩 조회
            Optional<Scrap> scrap = scrapRepository.findByUserIdAndVideoId(userId, video.getId());
            scrapId = scrap.map(Scrap::getId).orElse(null);

            // 관심 채널 조회
            favoriteChannelId = userDataService.getUserFavoriteChannelId(token, video.getChannelId());

            log.info("✅ 사용자 상태 조회 완료: scrapId={}, favoriteChannelId={}", scrapId, favoriteChannelId);
        }

        return new UserVideoStateResponse(scrapId, favoriteChannelId);
    }

    /**
     * 백엔드 분석 정보 조회
     */
    @Transactional(readOnly = true)
    public VideoAnalysisResponse getVideoAnalysis(String apiVideoId) {
        log.info("📊 백엔드 분석 정보 조회: apiVideoId={}", apiVideoId);

        Video video = videoRepository.findByApiVideoId(apiVideoId)
                .orElseThrow(() -> new BusinessException(VideoError.VIDEO_NOT_FOUND));

        try {
            List<DetailAnalysisDto.CommentHistogram> commentHistogram =
                    parseCommentHistogram(video.getCommentHistogram());

            List<DetailAnalysisDto.PopularTimestamp> popularTimestamps =
                    parsePopularTimestamps(video.getPopularTimestamps());

            List<CommentDto> topComments = getTopComments(video.getId());

            List<DetailAnalysisDto.SentimentFlow> sentimentFlow =
                    calculateSentimentFlow(video.getId());

            log.info("✅ 백엔드 분석 정보 조회 완료: apiVideoId={}", apiVideoId);

            return new VideoAnalysisResponse(
                    commentHistogram,
                    popularTimestamps,
                    topComments,
                    sentimentFlow
            );

        } catch (Exception e) {
            log.error("❌ 백엔드 분석 정보 파싱 실패: apiVideoId={}", apiVideoId, e);
            return new VideoAnalysisResponse(
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>()
            );
        }
    }

    /**
     * 전체 댓글 조회 (단순 DB 조회)
     */
    @Transactional(readOnly = true)
    public List<CommentDto> getVideoComments(String apiVideoId) {
        log.info("💬 전체 댓글 조회: apiVideoId={}", apiVideoId);

        Video video = videoRepository.findByApiVideoId(apiVideoId)
                .orElseThrow(() -> new BusinessException(VideoError.VIDEO_NOT_FOUND));

        if (video.isCommentsDisabled() || video.isHasNoComments()) {
            log.info("⚠️ 댓글 없음: apiVideoId={}", apiVideoId);
            return List.of();
        }

        List<Comment> comments = commentRepository.findByVideoId(video.getId());

        if (comments.isEmpty()) {
            log.warn("⏳ 댓글 수집 진행 중: apiVideoId={}", apiVideoId);
            return List.of();
        }

        log.info("✅ 댓글 조회 완료: apiVideoId={}, 댓글 수={}", apiVideoId, comments.size());

        return comments.stream()
                .map(this::toCommentDto)
                .collect(Collectors.toList());
    }

    // ========================================
    // 4. AI 분석 결과 조회
    // ========================================

    /**
     * AI 분석 결과 조회
     * - AI 요약
     * - 언어 분포
     * - 감정 분포
     * - 키워드
     */
    @Transactional(readOnly = true)
    public AIAnalysisResponse getAIAnalysis(String apiVideoId) {
        log.info("🤖 AI 분석 결과 조회: apiVideoId={}", apiVideoId);

        Video video = videoRepository.findByApiVideoId(apiVideoId)
                .orElseThrow(() -> new BusinessException(VideoError.VIDEO_NOT_FOUND));

        try {
            // 1. 언어 분포
            List<DetailAnalysisDto.LanguageDistribution> languageDistribution =
                    parseLanguageDistribution(video.getLanguageDistribution());

            // 2. 감정 분포
            DetailAnalysisDto.SentimentDistribution sentimentDistribution =
                    parseSentimentDistribution(video.getSentimentDistribution());

            // 3. 키워드
            List<String> keywords = parseKeywords(video.getKeywords());

            log.info("✅ AI 분석 결과 조회 완료: apiVideoId={}", apiVideoId);

            return new AIAnalysisResponse(
                    video.getSummation(),
                    video.isWarning(),
                    languageDistribution,
                    sentimentDistribution,
                    keywords
            );

        } catch (Exception e) {
            log.error("❌ AI 분석 결과 파싱 실패: apiVideoId={}, error={}",
                    apiVideoId, e.getMessage(), e);

            return new AIAnalysisResponse(
                    null,
                    false,
                    new ArrayList<>(),
                    new DetailAnalysisDto.SentimentDistribution(0, 0, 0),
                    new ArrayList<>()
            );
        }
    }

    // ========================================
    // 5. Deprecated - 통합 API (하위 호환)
    // ========================================

    /**
     * @deprecated 기존 통합 API
     * 프론트엔드 마이그레이션 후 제거 예정
     */
    @Deprecated
    @Transactional
    public DetailPageResponse getVideoDetail(String token, String apiVideoId) {
        log.warn("⚠️ Deprecated API 사용: getVideoDetail - 분리된 API 사용 권장");

        // 기본 정보
        VideoBasicResponse basic = getVideoBasic(token, apiVideoId);

        // 댓글 조회 (이때 수집됨)
        List<CommentDto> comments = getVideoComments(apiVideoId);

        // 백엔드 분석
        VideoAnalysisResponse backendAnalysis = getVideoAnalysis(apiVideoId);

        // AI 분석
        AIAnalysisResponse aiAnalysis = getAIAnalysis(apiVideoId);

        // DetailAnalysisDto로 통합
        DetailAnalysisDto analysis = new DetailAnalysisDto(
                aiAnalysis.summary(),
                aiAnalysis.isWarning(),
                backendAnalysis.topComments(),
                aiAnalysis.languageDistribution(),
                aiAnalysis.sentimentDistribution(),
                backendAnalysis.popularTimestamps(),
                backendAnalysis.commentHistogram(),
                aiAnalysis.keywords()
        );

        return new DetailPageResponse(
                basic.video(),
                basic.channel(),
                analysis,
                comments
        );
    }

    // ========================================
    // 6. 파싱 헬퍼 메서드
    // ========================================

    /**
     * 댓글 히스토그램 파싱
     */
    private List<DetailAnalysisDto.CommentHistogram> parseCommentHistogram(String json) {
        try {
            log.debug("댓글 히스토그램 파싱: json={}", json);

            if (json == null || json.trim().isEmpty() || json.equals("{}")) {
                log.debug("빈 히스토그램 데이터");
                return new ArrayList<>();
            }

            Map<String, Integer> map = objectMapper.readValue(json,
                    new TypeReference<Map<String, Integer>>() {});

            return map.entrySet().stream()
                    .map(e -> new DetailAnalysisDto.CommentHistogram(
                            e.getKey(),
                            e.getValue()
                    ))
                    .sorted(Comparator.comparing(h -> Integer.parseInt(h.hour())))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("댓글 히스토그램 파싱 실패: json={}, error={}", json, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 인기 타임스탬프 파싱
     */
    private List<DetailAnalysisDto.PopularTimestamp> parsePopularTimestamps(String json) {
        try {
            if (json == null || json.trim().isEmpty() || json.equals("{}")) {
                return new ArrayList<>();
            }

            Map<String, Integer> map = objectMapper.readValue(json,
                    new TypeReference<Map<String, Integer>>() {});

            return map.entrySet().stream()
                    .map(e -> new DetailAnalysisDto.PopularTimestamp(
                            e.getKey(),
                            e.getValue()
                    ))
                    .sorted(Comparator.comparing(DetailAnalysisDto.PopularTimestamp::mentionCount).reversed())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("인기 타임스탬프 파싱 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 언어 분포 파싱
     */
    private List<DetailAnalysisDto.LanguageDistribution> parseLanguageDistribution(String json) {
        try {
            if (json == null || json.trim().isEmpty()) {
                return new ArrayList<>();
            }

            Map<String, Integer> map = objectMapper.readValue(json,
                    new TypeReference<Map<String, Integer>>() {});

            return map.entrySet().stream()
                    .map(e -> new DetailAnalysisDto.LanguageDistribution(
                            e.getKey(),
                            e.getValue()
                    ))
                    .sorted(Comparator.comparing(DetailAnalysisDto.LanguageDistribution::ratio).reversed())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("언어 분포 파싱 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 감정 분포 파싱
     */
    private DetailAnalysisDto.SentimentDistribution parseSentimentDistribution(String json) {
        try {
            if (json == null || json.trim().isEmpty()) {
                return new DetailAnalysisDto.SentimentDistribution(0, 0, 0);
            }

            Map<String, Integer> map = objectMapper.readValue(json,
                    new TypeReference<Map<String, Integer>>() {});

            return new DetailAnalysisDto.SentimentDistribution(
                    map.getOrDefault("positive", 0),
                    map.getOrDefault("negative", 0),
                    map.getOrDefault("other", 0)
            );
        } catch (Exception e) {
            log.warn("감정 분포 파싱 실패: {}", e.getMessage());
            return new DetailAnalysisDto.SentimentDistribution(0, 0, 0);
        }
    }

    /**
     * 키워드 파싱
     */
    private List<String> parseKeywords(String json) {
        try {
            if (json == null || json.trim().isEmpty()) {
                return new ArrayList<>();
            }

            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("키워드 파싱 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 좋아요 TOP 5 댓글 조회
     */
    private List<CommentDto> getTopComments(Long videoId) {
        List<Comment> topComments = commentRepository
                .findTop5ByVideoIdOrderByLikeCountDesc(videoId);

        return topComments.stream()
                .map(comment -> {
                    String sentiment = comment.getSentimentType() != null
                            ? comment.getSentimentType().name()
                            : null;

                    List<String> detailSentiments = comment.getDetailSentiments() != null
                            ? comment.getDetailSentiments().stream()
                            .map(DetailSentimentType::name)
                            .collect(Collectors.toList())
                            : new ArrayList<>();

                    return new CommentDto(
                            comment.getApiCommentId(),
                            comment.getWriter(),
                            comment.getCommentContent(),
                            comment.getLikeCount(),
                            sentiment,
                            comment.getWrittenAt(),
                            false,  // 좋아요 Top 5 대댓글 유무는 항상 false
                            detailSentiments
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 감정 흐름 시간 구간별 집계
     * 전체 기간을 N등분하여 각 시간 구간의 감정 비율 계산
     */
    private List<DetailAnalysisDto.SentimentFlow> calculateSentimentFlow(Long videoId) {
        try {
            // AI 분석된 댓글만 조회
            List<Comment> comments = commentRepository.findByVideoId(videoId).stream()
                    .filter(c -> c.getSentimentType() != null)
                    .filter(c -> c.getWrittenAt() != null)
                    .sorted(Comparator.comparing(Comment::getWrittenAt))
                    .collect(Collectors.toList());

            if (comments.isEmpty()) {
                return new ArrayList<>();
            }

            int maxDataPoints = appConfig.getSentimentFlowMaxDataPoints();

            // 댓글이 적으면 일별 집계
            if (comments.size() <= maxDataPoints) {
                return calculateDailySentiment(comments);
            }

            // 시간 기반 구간 분할
            LocalDateTime startDate = parseDateTime(comments.get(0).getWrittenAt());
            LocalDateTime endDate = parseDateTime(comments.get(comments.size() - 1).getWrittenAt());

            long totalDays = ChronoUnit.DAYS.between(startDate, endDate);

            // 기간이 너무 짧으면 일별 집계
            if (totalDays < maxDataPoints) {
                return calculateDailySentiment(comments);
            }

            double daysPerSection = (double) totalDays / maxDataPoints;
            List<DetailAnalysisDto.SentimentFlow> flows = new ArrayList<>();

            log.info("📊 시간 기반 구간 분할: 전체={}일, 구간당={:.1f}일, {}개 구간",
                    totalDays, daysPerSection, maxDataPoints);

            for (int i = 0; i < maxDataPoints; i++) {
                LocalDateTime sectionStart = startDate.plusDays((long) (i * daysPerSection));
                LocalDateTime sectionEnd = (i == maxDataPoints - 1)
                        ? endDate.plusDays(1)
                        : startDate.plusDays((long) ((i + 1) * daysPerSection));

                // 해당 구간의 댓글 필터링
                List<Comment> sectionComments = comments.stream()
                        .filter(c -> {
                            LocalDateTime commentDate = parseDateTime(c.getWrittenAt());
                            return !commentDate.isBefore(sectionStart) && commentDate.isBefore(sectionEnd);
                        })
                        .collect(Collectors.toList());

                // 구간에 댓글이 없으면 스킵
                if (sectionComments.isEmpty()) {
                    log.debug("⚠️ 구간 {} 댓글 없음: {} ~ {}",
                            i + 1,
                            sectionStart.toLocalDate(),
                            sectionEnd.toLocalDate());
                    continue;
                }

                // 구간 내 감정 집계
                Map<SentimentType, Long> sentimentCounts = sectionComments.stream()
                        .collect(Collectors.groupingBy(
                                Comment::getSentimentType,
                                Collectors.counting()
                        ));

                long total = sentimentCounts.values().stream()
                        .mapToLong(Long::longValue)
                        .sum();

                int positive = (int) Math.round((double) sentimentCounts.getOrDefault(SentimentType.POSITIVE, 0L) * 100.0 / total);
                int negative = (int) Math.round((double) sentimentCounts.getOrDefault(SentimentType.NEGATIVE, 0L) * 100.0 / total);
                int other = 100 - positive - negative;

                // 구간의 중간 날짜를 대표 날짜로 사용
                LocalDateTime middleDate = sectionStart.plusDays((long) (daysPerSection / 2));
                String representativeDate = middleDate.toLocalDate().toString();

                flows.add(new DetailAnalysisDto.SentimentFlow(
                        representativeDate,
                        positive,
                        negative,
                        other
                ));

                log.debug("✅ 구간 {}: {} ~ {}, 댓글={}개, positive={}%, negative={}%, other={}%",
                        i + 1,
                        sectionStart.toLocalDate(),
                        sectionEnd.toLocalDate(),
                        sectionComments.size(),
                        positive,
                        negative,
                        other);
            }

            log.info("✅ 감정 흐름 시간 구간별 집계 완료: 전체={}개 댓글, {}개 구간",
                    comments.size(), flows.size());

            return flows;

        } catch (Exception e) {
            log.error("❌ 감정 흐름 계산 실패: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * ISO 8601 형식 날짜 파싱 (Z 타임존 처리)
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            // ISO_DATE_TIME 포맷터 사용 (Z 처리 가능)
            if (dateTimeStr.endsWith("Z")) {
                // UTC 시간을 LocalDateTime으로 변환
                return java.time.Instant.parse(dateTimeStr)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime();
            } else {
                return LocalDateTime.parse(dateTimeStr);
            }
        } catch (Exception e) {
            log.error("날짜 파싱 실패: {}", dateTimeStr, e);
            throw e;
        }
    }

    /**
     * 일별 감정 집계 (댓글이 적을 때)
     */
    private List<DetailAnalysisDto.SentimentFlow> calculateDailySentiment(List<Comment> comments) {
        Map<String, Map<SentimentType, Long>> dailySentiments = comments.stream()
                .collect(Collectors.groupingBy(
                        c -> extractDate(c.getWrittenAt()),
                        Collectors.groupingBy(
                                Comment::getSentimentType,
                                Collectors.counting()
                        )
                ));

        return dailySentiments.entrySet().stream()
                .map(entry -> {
                    String date = entry.getKey();
                    Map<SentimentType, Long> sentiments = entry.getValue();

                    long total = sentiments.values().stream()
                            .mapToLong(Long::longValue)
                            .sum();

                    int positive = (int) Math.round((double) sentiments.getOrDefault(SentimentType.POSITIVE, 0L) * 100.0 / total);
                    int negative = (int) Math.round((double) sentiments.getOrDefault(SentimentType.NEGATIVE, 0L) * 100.0 / total);
                    int other = 100 - positive - negative;


                    return new DetailAnalysisDto.SentimentFlow(
                            date,
                            positive,
                            negative,
                            other
                    );
                })
                .sorted(Comparator.comparing(DetailAnalysisDto.SentimentFlow::date))
                .collect(Collectors.toList());
    }

    /**
     * ISO 8601 날짜에서 yyyy-MM-dd 추출
     */
    private String extractDate(String isoDateTime) {
        try {
            return isoDateTime.substring(0, 10);
        } catch (Exception e) {
            return isoDateTime;
        }
    }

    // ========================================
    // 7. 공통 헬퍼 메서드
    // ========================================

    /**
     * VideoBasicResponse 생성
     */
    private VideoBasicResponse createBasicResponse(String token, Video video) {

        DetailVideoDto videoDto = new DetailVideoDto(
                video.getApiVideoId(),
                video.getTitle(),
                video.getDescription(),
                video.getUploadedAt(),
                video.getThumbnailUrl(),
                Long.parseLong(video.getViewCount()),
                Long.parseLong(video.getLikeCount()),
                Integer.parseInt(video.getCommentCount())
        );

        DetailChannelDto channelDto = new DetailChannelDto(
                video.getChannelId(),
                video.getChannelName(),
                video.getChannelThumbnailUrl(),
                Long.parseLong(video.getSubscriberCount())
        );

        return new VideoBasicResponse(videoDto, channelDto);
    }

    /**
     * Comment → CommentDto 변환
     */
    private CommentDto toCommentDto(Comment comment) {
        String sentiment = comment.getSentimentType() != null
                ? comment.getSentimentType().name()
                : null;

        List<String> detailSentiments = comment.getDetailSentiments() != null
                ? comment.getDetailSentiments().stream()
                .map(DetailSentimentType::name)
                .collect(Collectors.toList())
                : new ArrayList<>();

        boolean hasReplies = (comment.getHasReplies() != null) ? comment.getHasReplies() : false;

        return new CommentDto(
                comment.getApiCommentId(),
                comment.getWriter(),
                comment.getCommentContent(),
                comment.getLikeCount(),
                sentiment,
                comment.getWrittenAt(),
                hasReplies,
                detailSentiments
        );
    }

    private boolean shouldCheckDeletion(Video video) {
        if (video.isDeleted()) {
            return false;
        }
        if (video.getDeleteCheckedAt() == null) {
            return true;
        }
        LocalDateTime checkThreshold = LocalDateTime.now()
                .minusDays(appConfig.getDeletionCheckDays());
        return video.getDeleteCheckedAt().isBefore(checkThreshold);
    }

    private boolean shouldUpdateMetadata(Video video) {
        if (video.getLastMetadataUpdatedAt() == null) {
            return true;
        }
        LocalDateTime updateThreshold = LocalDateTime.now()
                .minusDays(appConfig.getMetadataUpdateDays());
        return video.getLastMetadataUpdatedAt().isBefore(updateThreshold);
    }

    private void checkAndUpdateDeletionStatus(Video video) {
        boolean isDeleted = videoService.checkIfVideoDeleted(video.getApiVideoId());
        if (isDeleted) {
            video.setDeleted(true);
            video.setDeleteCheckedAt(LocalDateTime.now());
            videoRepository.save(video);
            throw new BusinessException(VideoError.VIDEO_DELETED);
        }
        video.setDeleteCheckedAt(LocalDateTime.now());
        videoRepository.save(video);
    }

    private void updateMetadata(Video video) {
        try {
            VideoApiResponse videoInfo = videoService.getVideoInfo(video.getApiVideoId());

            video.setViewCount(videoInfo.viewCount());
            video.setLikeCount(videoInfo.likeCount());
            video.setCommentCount(videoInfo.commentCount());
            video.setLastMetadataUpdatedAt(LocalDateTime.now());
            video.setMetadataUpdateCount(video.getMetadataUpdateCount() + 1);

            videoRepository.save(video);

            log.info("✅ 메타데이터 갱신 완료: apiVideoId={}, updateCount={}",
                    video.getApiVideoId(), video.getMetadataUpdateCount());

        } catch (Exception e) {
            log.error("❌ 메타데이터 갱신 실패: apiVideoId={}, error={}",
                    video.getApiVideoId(), e.getMessage());
        }
    }
}