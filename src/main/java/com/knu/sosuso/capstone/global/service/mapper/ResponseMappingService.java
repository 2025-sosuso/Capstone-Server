package com.knu.sosuso.capstone.global.service.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.sosuso.capstone.domain.channel.dto.response.FavoriteVideoInfoResponse;
import com.knu.sosuso.capstone.domain.comment.dto.CommentDto;
import com.knu.sosuso.capstone.domain.comment.entity.Comment;
import com.knu.sosuso.capstone.domain.common.dto.*;
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
    private final CommentMapper commentMapper;

    /**
     * DB Video → VideoSummaryResponse 변환
     */
    @Transactional(readOnly = true)
    public VideoSummaryResponse mapDbToVideoSummaryResponse(String token, Video video) {
        try {
            // 영상 기본 정보
            VideoBasicDto videoDto = new VideoBasicDto(
                    video.getApiVideoId(),
                    video.getTitle(),
                    video.getDescription(),
                    video.getUploadedAt(),
                    video.getThumbnailUrl(),
                    parseLongOrDefault(video.getViewCount(), 0L),
                    parseLongOrDefault(video.getLikeCount(), 0L),
                    parseIntOrDefault(video.getCommentCount(), 0)
            );

            // 채널 기본 정보
            Long favoriteChannelId = userDataService.getUserFavoriteChannelId(token, video.getChannelId());
            ChannelBasicDto channelDto = new ChannelBasicDto(
                    video.getChannelId(),
                    video.getChannelName(),
                    video.getChannelThumbnailUrl(),
                    parseLongOrDefault(video.getSubscriberCount(), 0L)
            );

            // 분석 정보 (AI 분석)
            SentimentDistribution sentimentDto = parseSentimentDistribution(video.getSentimentDistribution());
            List<String> keywords = parseKeywords(video.getKeywords());

            VideoSummaryResponse.Analysis analysisDto = new VideoSummaryResponse.Analysis(
                    video.getSummation(),
                    sentimentDto,
                    keywords
            );

            return new VideoSummaryResponse(videoDto, channelDto, analysisDto);

        } catch (Exception e) {
            log.error("VideoSummaryResponse 변환 실패: videoId={}, error={}", video.getId(), e.getMessage(), e);
            throw new BusinessException(CommonError.DATA_CONVERSION_ERROR);
        }
    }

    /**
     * DB Video → FavoriteVideoInfoResponse 변환
     */
    @Transactional(readOnly = true)
    public FavoriteVideoInfoResponse mapDbToFavoriteVideoInfoResponse(String token, Video video) {
        try {
            // 영상 정보
            FavoriteVideoInfoResponse.Video videoDto = new FavoriteVideoInfoResponse.Video(
                    video.getApiVideoId(),
                    video.getTitle(),
                    video.getDescription(),
                    video.getUploadedAt(),
                    video.getThumbnailUrl(),
                    parseLongOrDefault(video.getViewCount(), 0L),
                    parseLongOrDefault(video.getLikeCount(), 0L),
                    parseIntOrDefault(video.getCommentCount(), 0)
            );

            // 채널 정보
            FavoriteVideoInfoResponse.Channel channelDto = new FavoriteVideoInfoResponse.Channel(
                    video.getChannelId(),
                    video.getChannelName(),
                    video.getChannelThumbnailUrl(),
                    parseLongOrDefault(video.getSubscriberCount(), 0L)
            );

            // 분석 정보
            SentimentDistribution sentimentDto = parseSentimentDistribution(video.getSentimentDistribution());
            List<String> keywords = parseKeywords(video.getKeywords());
            List<CommentDto> topComments = mapToTopCommentsFromDb(video.getId());

            FavoriteVideoInfoResponse.Analysis analysisDto = new FavoriteVideoInfoResponse.Analysis(
                    video.getSummation(),
                    sentimentDto,
                    keywords,
                    topComments
            );

            return new FavoriteVideoInfoResponse(videoDto, channelDto, analysisDto);

        } catch (Exception e) {
            log.error("FavoriteVideoInfoResponse 변환 실패: videoId={}, error={}", video.getId(), e.getMessage(), e);
            throw new BusinessException(CommonError.DATA_CONVERSION_ERROR);
        }
    }

    // ========================================
    // 🔧 내부 헬퍼 메서드들
    // ========================================

    /**
     * DB에서 좋아요 TOP 5 댓글 추출
     */
    @Transactional(readOnly = true)
    public List<CommentDto> mapToTopCommentsFromDb(Long videoId) {
        List<Comment> topComments = commentRepository.findByVideoIdOrderByLikeCountDesc(videoId).stream()
                .limit(appConfig.getTopCommentsCount())
                .collect(Collectors.toList());

        return commentMapper.toTopCommentDtoList(topComments);
    }

    /**
     * JSON 감정 분포 파싱
     */
    private SentimentDistribution parseSentimentDistribution(String sentimentJson) {
        try {
            if (sentimentJson == null || sentimentJson.trim().isEmpty()) {
                return new SentimentDistribution(0, 0, 0);
            }

            Map<String, Integer> sentimentRatio = objectMapper.readValue(
                    sentimentJson,
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Integer.class)
            );

            return new SentimentDistribution(
                    sentimentRatio.getOrDefault("positive", 0),
                    sentimentRatio.getOrDefault("negative", 0),
                    sentimentRatio.getOrDefault("other", 0)
            );

        } catch (Exception e) {
            log.warn("감정 분포 파싱 실패: {}", e.getMessage());
            return new SentimentDistribution(0, 0, 0);
        }
    }

    /**
     * JSON 키워드 파싱
     */
    private List<String> parseKeywords(String keywordsJson) {
        try {
            if (keywordsJson == null || keywordsJson.trim().isEmpty()) {
                return List.of();
            }
            return objectMapper.readValue(keywordsJson, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("키워드 파싱 실패: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * String → Long 변환 (안전)
     */
    private Long parseLongOrDefault(String value, Long defaultValue) {
        try {
            return (value != null && !value.isEmpty()) ? Long.parseLong(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * String → Integer 변환 (안전)
     */
    private Integer parseIntOrDefault(String value, Integer defaultValue) {
        try {
            return (value != null && !value.isEmpty()) ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}