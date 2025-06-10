package com.knu.sosuso.capstone.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.sosuso.capstone.ai.dto.AIAnalysisResponse;
import com.knu.sosuso.capstone.domain.Comment;
import com.knu.sosuso.capstone.domain.Video;
import com.knu.sosuso.capstone.dto.response.CommentApiResponse;
import com.knu.sosuso.capstone.dto.response.VideoApiResponse;
import com.knu.sosuso.capstone.dto.response.detail.*;
import com.knu.sosuso.capstone.repository.CommentRepository;
import com.knu.sosuso.capstone.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class ResponseMappingService {

    private final ObjectMapper objectMapper;
    private final CommentRepository commentRepository;
    private final VideoRepository videoRepository;
    private final UserDataService userDataService;

    /**
     * YouTube API 데이터를 SearchResultResponse로 변환 (새로운 데이터)
     */
    public DetailPageResponse mapToSearchResult(
            String token,
            VideoApiResponse videoInfo,
            CommentApiResponse commentInfo,
            AIAnalysisResponse analysisResponse) {

        DetailVideoDto video = mapToVideoResponse(token, videoInfo);
        DetailChannelDto channel = mapToChannelResponse(token, videoInfo);
        DetailAnalysisDto analysis = mapToAnalysisResponse(commentInfo, analysisResponse);
        List<DetailCommentDto> comments = mapToCommentResponses(commentInfo.allComments(), analysisResponse);

        return new DetailPageResponse(video, channel, analysis, comments);
    }

    /**
     * DB 데이터를 SearchResultResponse로 변환 (기존 데이터)
     */
    public DetailPageResponse mapFromDbToSearchResult(String token, Video video) {
        try {
            DetailVideoDto detailVideoDto = mapDbVideoToVideoResponse(token, video);
            DetailChannelDto detailChannelDto = mapDbVideoToChannelResponse(token, video);
            DetailAnalysisDto detailAnalysisDto = mapDbVideoToAnalysisResponse(video);
            List<DetailCommentDto> detailCommentDtos = mapDbCommentsToCommentResponses(video.getId());

            return new DetailPageResponse(detailVideoDto, detailChannelDto, detailAnalysisDto, detailCommentDtos);

        } catch (Exception e) {
            log.error("DB 데이터 매핑 실패: videoId={}, error={}", video.getId(), e.getMessage());
            throw new RuntimeException("DB 데이터 매핑 중 오류 발생", e);
        }
    }

    /**
     * VideoApiResponse -> VideoResponse 변환
     */
    public DetailVideoDto mapToVideoResponse(String token, VideoApiResponse videoInfo) {
        Long scrapId = userDataService.getUserScrapId(token, videoInfo.apiVideoId());

        return new DetailVideoDto(
                videoInfo.apiVideoId(),
                videoInfo.title(),
                videoInfo.description(),
                videoInfo.publishedAt(),
                videoInfo.thumbnailUrl(),
                parseLong(videoInfo.viewCount()),
                parseLong(videoInfo.likeCount()),
                parseInt(videoInfo.commentCount()),
                scrapId
        );
    }

    /**
     * VideoApiResponse -> ChannelResponse 변환
     */
    public DetailChannelDto mapToChannelResponse(String token, VideoApiResponse videoInfo) {
        Long favoriteChannelId = userDataService.getUserFavoriteChannelId(token, videoInfo.channelId());

        return new DetailChannelDto(
                videoInfo.channelId(),
                videoInfo.channelTitle(),
                videoInfo.channelThumbnailUrl(),
                parseLong(videoInfo.subscriberCount()),
                favoriteChannelId
        );
    }

    /**
     * DB Video -> VideoResponse 변환
     */
    private DetailVideoDto mapDbVideoToVideoResponse(String token, Video video) {
        Long scrapId = userDataService.getUserScrapId(token, video.getApiVideoId());

        return new DetailVideoDto(
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
    }

    /**
     * DB Video -> ChannelResponse 변환
     */
    private DetailChannelDto mapDbVideoToChannelResponse(String token, Video video) {
        Long favoriteChannelId = userDataService.getUserFavoriteChannelId(token, video.getChannelId());

        return new DetailChannelDto(
                video.getChannelId(),
                video.getChannelName(),
                video.getChannelThumbnailUrl(),
                parseLong(video.getSubscriberCount()),
                favoriteChannelId
        );
    }

    /**
     * CommentApiResponse + AIAnalysisResponse -> AnalysisResponse 변환
     */
    private DetailAnalysisDto mapToAnalysisResponse(
            CommentApiResponse commentInfo,
            AIAnalysisResponse analysisResponse) {

        if (analysisResponse == null) {
            // AI 분석이 없는 경우 - 백엔드 처리 데이터는 채우고, AI 데이터는 빈 값
            return new DetailAnalysisDto(
                    null,  // summary = null
                    false, // isWarning = false
                    mapToTopCommentsFromCommentData(commentInfo.allComments(), null), // 좋아요 TOP5 백엔드 처리 데이터
                    List.of(), // languageDistribution = 빈 리스트
                    new DetailAnalysisDto.SentimentDistribution(0.0, 0.0, 0.0), // sentimentDistribution = 빈 값
                    mapToPopularTimestamps(commentInfo.popularTimestamps()), // 백엔드 처리 데이터
                    mapToCommentHistogram(commentInfo.commentHistogram()),   // 백엔드 처리 데이터
                    List.of() // keywords = 빈 리스트
            );
        }

        // AI 분석 성공한 경우
        List<DetailAnalysisDto.LanguageDistribution> languageDistribution =
                analysisResponse.languageRatio().entrySet().stream()
                        .map(entry -> new DetailAnalysisDto.LanguageDistribution(entry.getKey(), entry.getValue()))
                        .collect(Collectors.toList());

        DetailAnalysisDto.SentimentDistribution sentimentDistribution =
                new DetailAnalysisDto.SentimentDistribution(
                        analysisResponse.sentimentRatio().getOrDefault("positive", 0.0),
                        analysisResponse.sentimentRatio().getOrDefault("negative", 0.0),
                        analysisResponse.sentimentRatio().getOrDefault("other", 0.0)
                );

        return new DetailAnalysisDto(
                analysisResponse.summation(),
                analysisResponse.isWarning(),
                mapToTopCommentsFromCommentData(commentInfo.allComments(), analysisResponse),
                languageDistribution,
                sentimentDistribution,
                mapToPopularTimestamps(commentInfo.popularTimestamps()),
                mapToCommentHistogram(commentInfo.commentHistogram()),
                analysisResponse.keywords()
        );
    }

    /**
     * DB Video -> AnalysisResponse 변환
     */
    private DetailAnalysisDto mapDbVideoToAnalysisResponse(Video video) {
        try {
            // 백엔드 분석 데이터 (항상 있음)
            Map<Integer, Integer> commentHistogramData = parseJsonToMap(video.getCommentHistogram(), Integer.class, Integer.class);
            Map<String, Integer> popularTimestampsData = parseJsonToMap(video.getPopularTimestamps(), String.class, Integer.class);

            List<DetailAnalysisDto.PopularTimestamp> popularTimestamps = mapToPopularTimestamps(popularTimestampsData);
            List<DetailAnalysisDto.CommentHistogram> commentHistogram = mapToCommentHistogram(commentHistogramData);

            // AI 분석 완료 여부 체크
            boolean hasAIAnalysis = video.getSummation() != null &&
                    video.getLanguageDistribution() != null &&
                    video.getSentimentDistribution() != null &&
                    video.getKeywords() != null;

            if (!hasAIAnalysis) {
                // AI 분석 미완료
                return new DetailAnalysisDto(
                        null, false, mapToTopCommentsFromDb(video.getId()), List.of(),
                        new DetailAnalysisDto.SentimentDistribution(0.0, 0.0, 0.0),
                        popularTimestamps, commentHistogram, List.of()
                );
            }

            // AI 분석 완료
            Map<String, Double> languageRatio = parseJsonToMap(video.getLanguageDistribution(), String.class, Double.class);
            Map<String, Double> sentimentRatio = parseJsonToMap(video.getSentimentDistribution(), String.class, Double.class);
            List<String> keywords = objectMapper.readValue(video.getKeywords(), new TypeReference<List<String>>() {
            });

            List<DetailAnalysisDto.LanguageDistribution> languageDistribution =
                    languageRatio.entrySet().stream()
                            .map(entry -> new DetailAnalysisDto.LanguageDistribution(entry.getKey(), entry.getValue()))
                            .collect(Collectors.toList());

            DetailAnalysisDto.SentimentDistribution sentimentDistribution =
                    new DetailAnalysisDto.SentimentDistribution(
                            sentimentRatio.getOrDefault("positive", 0.0),
                            sentimentRatio.getOrDefault("negative", 0.0),
                            sentimentRatio.getOrDefault("other", 0.0)
                    );

            return new DetailAnalysisDto(
                    video.getSummation(),
                    video.isWarning(),
                    mapToTopCommentsFromDb(video.getId()),
                    languageDistribution,
                    sentimentDistribution,
                    popularTimestamps,
                    commentHistogram,
                    keywords
            );

        } catch (Exception e) {
            log.error("DB AnalysisResponse 매핑 실패: videoId={}, error={}", video.getId(), e.getMessage());
            throw new RuntimeException("DB 분석 데이터 매핑 중 오류 발생", e);
        }
    }

    /**
     * 댓글 리스트 변환 (관련도 순서 유지)
     */
    private List<DetailCommentDto> mapToCommentResponses(
            List<CommentApiResponse.CommentData> commentDataList,
            AIAnalysisResponse analysisResponse) {

        log.info("댓글 매핑 시작: 입력 댓글 수={}, AI분석 결과={}",
                commentDataList != null ? commentDataList.size() : 0,
                analysisResponse != null ? "있음" : "없음");

        List<DetailCommentDto> result = commentDataList.stream()
                .map(commentData -> {
                    if (analysisResponse != null) {
                        return mapToCommentResponseWithAI(commentData, analysisResponse);
                    } else {
                        return mapToCommentResponseWithoutAI(commentData);
                    }
                })
                .collect(Collectors.toList());

        log.info("댓글 매핑 완료: 출력 댓글 수={}", result.size());
        return result;
    }

    /**
     * DB 댓글을 CommentResponse로 변환
     */
    private List<DetailCommentDto> mapDbCommentsToCommentResponses(Long videoId) {
        return commentRepository.findByVideoIdOrderByIdAsc(videoId).stream()
                .map(comment -> new DetailCommentDto(
                        comment.getApiCommentId(),
                        comment.getWriter(),
                        comment.getCommentContent(),
                        comment.getLikeCount(),
                        comment.getSentimentType() != null ? comment.getSentimentType().name().toUpperCase() : null,
                        comment.getWrittenAt()
                ))
                .collect(Collectors.toList());
    }

    /**
     * AI 분석 결과가 있는 경우 댓글 변환
     */
    private DetailCommentDto mapToCommentResponseWithAI(
            CommentApiResponse.CommentData commentData,
            AIAnalysisResponse analysisResponse) {
        String sentiment = null;
        if (analysisResponse.sentimentComments().containsKey(commentData.id())) {
            sentiment = analysisResponse.sentimentComments().get(commentData.id()).name().toUpperCase();
        }

        return new DetailCommentDto(
                commentData.id(),
                commentData.authorName(),
                commentData.commentText(),
                commentData.likeCount(),
                sentiment,
                commentData.publishedAt()
        );
    }

    /**
     * AI 분석 결과가 없는 경우 댓글 변환
     */
    private DetailCommentDto mapToCommentResponseWithoutAI(CommentApiResponse.CommentData commentData) {
        return new DetailCommentDto(
                commentData.id(),
                commentData.authorName(),
                commentData.commentText(),
                commentData.likeCount(),
                null,
                commentData.publishedAt()
        );
    }

    /**
     * CommentData에서 좋아요 TOP 5 댓글 추출
     */
    private List<DetailCommentDto> mapToTopCommentsFromCommentData(
            List<CommentApiResponse.CommentData> commentDataList,
            AIAnalysisResponse analysisResponse) {

        return commentDataList.stream()
                .sorted((c1, c2) -> Integer.compare(c2.likeCount(), c1.likeCount()))
                .limit(5)
                .map(commentData -> {
                    if (analysisResponse != null) {
                        return mapToCommentResponseWithAI(commentData, analysisResponse);
                    } else {
                        return mapToCommentResponseWithoutAI(commentData);
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * DB에서 좋아요 TOP 5 댓글 추출 (public 메서드로 추가)
     */
    public List<Comment> mapToTopCommentsFromDb(String apiVideoId) {

        Optional<Video> videoOpt  = videoRepository.findByApiVideoId(apiVideoId);

        if (videoOpt.isPresent()) {
            Long videoId = videoOpt.get().getId();

            return commentRepository
                    .findByVideoIdOrderByLikeCountDesc(videoId)
                    .stream()
                    .limit(5)
                    .collect(Collectors.toList());
        }
        return null;
    }

    /**
     * DB에서 좋아요 TOP 5 댓글 추출
     */
    private List<DetailCommentDto> mapToTopCommentsFromDb(Long videoId) {
        return mapDbCommentsToCommentResponses(videoId).stream()
                .sorted((c1, c2) -> Integer.compare(c2.likeCount(), c1.likeCount()))
                .limit(5)
                .collect(Collectors.toList());
    }

    private List<DetailAnalysisDto.PopularTimestamp> mapToPopularTimestamps(Map<String, Integer> popularTimestampsData) {
        return popularTimestampsData.entrySet().stream()
                .map(entry -> new DetailAnalysisDto.PopularTimestamp(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private List<DetailAnalysisDto.CommentHistogram> mapToCommentHistogram(Map<Integer, Integer> commentHistogramData) {
        return commentHistogramData.entrySet().stream()
                .map(entry -> new DetailAnalysisDto.CommentHistogram(String.valueOf(entry.getKey()), entry.getValue()))
                .collect(Collectors.toList());
    }

    private <K, V> Map<K, V> parseJsonToMap(String json, Class<K> keyClass, Class<V> valueClass) throws Exception {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        return objectMapper.readValue(json, objectMapper.getTypeFactory().constructMapType(Map.class, keyClass, valueClass));
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