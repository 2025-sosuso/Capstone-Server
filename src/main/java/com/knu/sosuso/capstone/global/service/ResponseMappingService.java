package com.knu.sosuso.capstone.global.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.sosuso.capstone.domain.ai.dto.AIAnalysisResponse;
import com.knu.sosuso.capstone.domain.comment.dto.CommentDto;
import com.knu.sosuso.capstone.domain.comment.entity.value.CommentSentimentDetail;
import com.knu.sosuso.capstone.domain.detail.dto.*;
import com.knu.sosuso.capstone.domain.comment.dto.response.CommentApiResponse;
import com.knu.sosuso.capstone.domain.comment.repository.CommentRepository;
import com.knu.sosuso.capstone.domain.video.dto.response.VideoSummaryResponse;
import com.knu.sosuso.capstone.domain.video.entity.Video;
import com.knu.sosuso.capstone.domain.video.dto.response.VideoApiResponse;
import com.knu.sosuso.capstone.domain.video.service.UserDataService;
import com.knu.sosuso.capstone.global.exception.BusinessException;
import com.knu.sosuso.capstone.global.exception.error.CommonError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class ResponseMappingService {
    private final ObjectMapper objectMapper;
    private final CommentRepository commentRepository;
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
        List<CommentDto> comments = mapToCommentResponses(commentInfo.allComments(), analysisResponse);

        return new DetailPageResponse(video, channel, analysis, comments);
    }

    /**
     * DB 데이터를 SearchResultResponse로 변환 (기존 데이터)
     */
    @Transactional
    public DetailPageResponse mapFromDbToSearchResult(String token, Video video) {
        try {
            DetailVideoDto detailVideoDto = mapDbVideoToVideoResponse(token, video);
            DetailChannelDto detailChannelDto = mapDbVideoToChannelResponse(token, video);
            DetailAnalysisDto detailAnalysisDto = mapDbVideoToAnalysisResponse(video);
            List<CommentDto> commentDtos = mapDbCommentsToCommentResponses(video.getId());

            return new DetailPageResponse(detailVideoDto, detailChannelDto, detailAnalysisDto, commentDtos);

        } catch (Exception e) {
            log.error("DB 데이터 매핑 실패: videoId={}, error={}", video.getId(), e.getMessage());
            throw new BusinessException(CommonError.DATA_CONVERSION_ERROR);
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
    @Transactional
    public DetailAnalysisDto mapDbVideoToAnalysisResponse(Video video) {
        try {
            // 백엔드 분석 데이터 (항상 있음)
            Map<Integer, Integer> commentHistogramData = parseJsonToMap(video.getCommentHistogram(), Integer.class, Integer.class);
            Map<String, Integer> popularTimestampsData = parseJsonToMap(video.getPopularTimestamps(), String.class, Integer.class);

            List<DetailAnalysisDto.CommentHistogram> commentHistogram =
                    commentHistogramData.entrySet().stream()
                            .map(e -> new DetailAnalysisDto.CommentHistogram(String.valueOf(e.getKey()), e.getValue()))
                            .collect(Collectors.toList());

            List<DetailAnalysisDto.PopularTimestamp> popularTimestamps =
                    popularTimestampsData.entrySet().stream()
                            .map(e -> new DetailAnalysisDto.PopularTimestamp(e.getKey(), e.getValue()))
                            .collect(Collectors.toList());

            // AI 분석 데이터 (없을 수 있음)
            Map<String, Double> languageRatio = parseJsonToMap(video.getLanguageDistribution(), String.class, Double.class);
            Map<String, Double> sentimentRatio = parseJsonToMap(video.getSentimentDistribution(), String.class, Double.class);

            List<DetailAnalysisDto.LanguageDistribution> languageDistribution =
                    languageRatio.entrySet().stream()
                            .map(e -> new DetailAnalysisDto.LanguageDistribution(e.getKey(), e.getValue()))
                            .collect(Collectors.toList());

            DetailAnalysisDto.SentimentDistribution sentimentDistribution =
                    new DetailAnalysisDto.SentimentDistribution(
                            sentimentRatio.getOrDefault("positive", 0.0),
                            sentimentRatio.getOrDefault("negative", 0.0),
                            sentimentRatio.getOrDefault("other", 0.0)
                    );

            List<String> keywords = List.of();
            try {
                if (video.getKeywords() != null && !video.getKeywords().trim().isEmpty()) {
                    keywords = objectMapper.readValue(video.getKeywords(), new TypeReference<>() {
                    });
                }
            } catch (Exception e) {
                log.warn("키워드 파싱 실패: {}", e.getMessage());
            }

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
            throw new BusinessException(CommonError.DATA_CONVERSION_ERROR);
        }
    }

    /**
     * DetailPageResponse를 VideoSummaryResponse로 변환
     */
    public VideoSummaryResponse convertToVideoSummaryResponse(DetailPageResponse detailResponse) {
        try {
            var video = detailResponse.video();
            var channel = detailResponse.channel();
            var analysis = detailResponse.analysis();

            VideoSummaryResponse.Video videoDto = new VideoSummaryResponse.Video(
                    video.id(),
                    video.title(),
                    video.description(),
                    video.publishedAt(),
                    video.thumbnailUrl(),
                    video.viewCount(),
                    video.likeCount(),
                    video.commentCount()
            );

            VideoSummaryResponse.Channel channelDto = new VideoSummaryResponse.Channel(
                    channel.id(),
                    channel.title(),
                    channel.thumbnailUrl(),
                    channel.subscriberCount()
            );

            VideoSummaryResponse.SentimentDistribution sentimentDto = null;
            if (analysis != null && analysis.sentimentDistribution() != null) {
                var s = analysis.sentimentDistribution();
                sentimentDto = new VideoSummaryResponse.SentimentDistribution(
                        s.positive(), s.negative(), s.other()
                );
            }

            List<String> keywords = (analysis != null && analysis.keywords() != null)
                    ? analysis.keywords()
                    : List.of();

            String summary = (analysis != null) ? analysis.summary() : null;

            VideoSummaryResponse.Analysis analysisDto = new VideoSummaryResponse.Analysis(
                    summary,
                    sentimentDto,
                    keywords
            );

            return new VideoSummaryResponse(videoDto, channelDto, analysisDto);

        } catch (Exception e) {
            log.error("VideoSummaryResponse 변환 실패: error={}", e.getMessage(), e);
            throw new BusinessException(CommonError.DATA_CONVERSION_ERROR);
        }
    }

    /**
     * 댓글 리스트 변환 (관련도 순서 유지)
     */
    private List<CommentDto> mapToCommentResponses(
            List<CommentApiResponse.CommentData> commentDataList,
            AIAnalysisResponse analysisResponse) {

        log.info("댓글 매핑 시작: 입력 댓글 수={}, AI분석 결과={}",
                commentDataList != null ? commentDataList.size() : 0,
                analysisResponse != null ? "있음" : "없음");

        List<CommentDto> result = commentDataList.stream()
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
    private List<CommentDto> mapDbCommentsToCommentResponses(Long videoId) {
        return commentRepository.findByVideoIdOrderByIdAsc(videoId).stream()
                .map(comment -> new CommentDto(
                        comment.getApiCommentId(),
                        comment.getWriter(),
                        comment.getCommentContent(),
                        comment.getLikeCount(),
                        comment.getSentimentType() != null ? comment.getSentimentType().name().toUpperCase() : null,
                        comment.getWrittenAt(),
                        comment.getHasReplies() != null && comment.getHasReplies()
                ))
                .collect(Collectors.toList());
    }

    /**
     * AI 분석 결과가 있는 경우 댓글 변환
     */
    private CommentDto mapToCommentResponseWithAI(
            CommentApiResponse.CommentData commentData,
            AIAnalysisResponse analysisResponse) {

        // List에서 해당 댓글의 감정 분석 결과 찾기
        CommentSentimentDetail sentimentDetail = analysisResponse.sentimentComments().stream()
                .filter(detail -> detail.apiCommentId().equals(commentData.id()))
                .findFirst()
                .orElse(null);

        String sentiment = null;
        if (sentimentDetail != null) {
            sentiment = sentimentDetail.sentimentType().name().toUpperCase();
        }

        return new CommentDto(
                commentData.id(),
                commentData.authorName(),
                commentData.commentText(),
                commentData.likeCount(),
                sentiment,
                commentData.publishedAt(),
                commentData.hasReplies()
        );
    }
    /**
     * AI 분석 결과가 없는 경우 댓글 변환
     */
    private CommentDto mapToCommentResponseWithoutAI(CommentApiResponse.CommentData commentData) {
        return new CommentDto(
                commentData.id(),
                commentData.authorName(),
                commentData.commentText(),
                commentData.likeCount(),
                null,
                commentData.publishedAt(),
                commentData.hasReplies()
        );
    }

    /**
     * CommentData에서 좋아요 TOP 5 댓글 추출
     * TOP 5 댓글은 hasReplies를 무조건 false로 설정
     */
    private List<CommentDto> mapToTopCommentsFromCommentData(
            List<CommentApiResponse.CommentData> commentDataList,
            AIAnalysisResponse analysisResponse) {

        return commentDataList.stream()
                .sorted((c1, c2) -> Integer.compare(c2.likeCount(), c1.likeCount()))
                .limit(5)
                .map(commentData -> {
                    String sentiment = null;
                    if (analysisResponse != null) {
                        // List에서 해당 댓글의 감정 분석 결과 찾기
                        CommentSentimentDetail sentimentDetail = analysisResponse.sentimentComments().stream()
                                .filter(detail -> detail.apiCommentId().equals(commentData.id()))
                                .findFirst()
                                .orElse(null);

                        if (sentimentDetail != null) {
                            sentiment = sentimentDetail.sentimentType().name().toUpperCase();
                        }
                    }

                    // TOP 5 댓글은 hasReplies를 무조건 false로 설정
                    return new CommentDto(
                            commentData.id(),
                            commentData.authorName(),
                            commentData.commentText(),
                            commentData.likeCount(),
                            sentiment,
                            commentData.publishedAt(),
                            false  // TOP 5 댓글은 항상 false
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * DB에서 좋아요 TOP 5 댓글 추출
     * TOP 5 댓글은 hasReplies를 무조건 false로 설정
     */
    @Transactional
    public List<CommentDto> mapToTopCommentsFromDb(Long videoId) {
        return commentRepository.findByVideoIdOrderByLikeCountDesc(videoId).stream()
                .limit(5)
                .map(comment -> new CommentDto(
                        comment.getApiCommentId(),
                        comment.getWriter(),
                        comment.getCommentContent(),
                        comment.getLikeCount(),
                        comment.getSentimentType() != null ? comment.getSentimentType().name().toUpperCase() : null,
                        comment.getWrittenAt(),
                        false  // TOP 5 댓글은 항상 false
                ))
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