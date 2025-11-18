package com.knu.sosuso.capstone.global.service.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.sosuso.capstone.domain.comment.dto.CommentDto;
import com.knu.sosuso.capstone.domain.comment.entity.Comment;
import com.knu.sosuso.capstone.domain.detail.dto.*;
import com.knu.sosuso.capstone.domain.comment.repository.CommentRepository;
import com.knu.sosuso.capstone.domain.video.dto.response.VideoSummaryResponse;
import com.knu.sosuso.capstone.domain.video.entity.Video;
import com.knu.sosuso.capstone.domain.video.service.UserDataService;
import com.knu.sosuso.capstone.global.config.AppConfig;
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
    private final AppConfig appConfig;
    private final VideoMapper videoMapper;
    private final CommentMapper commentMapper;

    /**
     * DB 데이터를 SearchResultResponse로 변환 (기존 데이터)
     */
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
     * DB Video -> VideoResponse 변환
     */
    private DetailVideoDto mapDbVideoToVideoResponse(String token, Video video) {
        Long scrapId = userDataService.getUserScrapId(token, video.getApiVideoId());
        return videoMapper.toDetailVideoDto(video, scrapId);
    }

    /**
     * DB Video -> ChannelResponse 변환
     */
    private DetailChannelDto mapDbVideoToChannelResponse(String token, Video video) {
        Long favoriteChannelId = userDataService.getUserFavoriteChannelId(token, video.getChannelId());
        return videoMapper.toDetailChannelDto(video, favoriteChannelId);
    }

    /**
     * DB Video -> AnalysisResponse 변환
     */
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
            Map<String, Integer> languageRatio = parseJsonToMap(video.getLanguageDistribution(), String.class, Integer.class);
            Map<String, Integer> sentimentRatio = parseJsonToMap(video.getSentimentDistribution(), String.class, Integer.class);

            List<DetailAnalysisDto.LanguageDistribution> languageDistribution =
                    languageRatio.entrySet().stream()
                            .map(e -> new DetailAnalysisDto.LanguageDistribution(
                                    e.getKey(),
                                    e.getValue()
                            ))
                            .collect(Collectors.toList());

            DetailAnalysisDto.SentimentDistribution sentimentDistribution =
                    new DetailAnalysisDto.SentimentDistribution(
                            sentimentRatio.getOrDefault("positive", 0),
                            sentimentRatio.getOrDefault("negative", 0),
                            sentimentRatio.getOrDefault("other", 0)
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
     * DB 댓글을 CommentResponse로 변환
     */
    private List<CommentDto> mapDbCommentsToCommentResponses(Long videoId) {
        List<Comment> comments = commentRepository.findByVideoIdOrderByIdAsc(videoId);
        return commentMapper.toDtoList(comments);
    }

    /**
     * DB에서 좋아요 TOP 5 댓글 추출
     * TOP 5 댓글은 hasReplies를 무조건 false로 설정
     */
    @Transactional(readOnly = true)
    public List<CommentDto> mapToTopCommentsFromDb(Long videoId) {
        List<Comment> topComments = commentRepository.findByVideoIdOrderByLikeCountDesc(videoId).stream()
                .limit(appConfig.getTopCommentsCount())
                .collect(Collectors.toList());

        return commentMapper.toTopCommentDtoList(topComments);
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
}