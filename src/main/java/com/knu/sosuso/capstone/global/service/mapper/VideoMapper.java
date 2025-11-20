package com.knu.sosuso.capstone.global.service.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.sosuso.capstone.domain.common.dto.ChannelBasicDto;
import com.knu.sosuso.capstone.domain.common.dto.VideoBasicDto;
import com.knu.sosuso.capstone.domain.detail.dto.DetailChannelDto;
import com.knu.sosuso.capstone.domain.detail.dto.DetailVideoDto;
import com.knu.sosuso.capstone.domain.video.dto.response.VideoSummaryResponse;
import com.knu.sosuso.capstone.domain.video.entity.Video;
import com.knu.sosuso.capstone.global.exception.BusinessException;
import com.knu.sosuso.capstone.global.exception.error.CommonError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Video 엔티티와 다양한 응답 DTO 간의 변환을 담당하는 Mapper
 *
 * 중복된 변환 로직을 제거하고 일관성 있는 변환 제공
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoMapper {

    private final ObjectMapper objectMapper;

    /**
     * Video -> VideoSummaryResponse 변환
     *
     * @param video Video 엔티티
     * @param scrapId 스크랩 ID (nullable)
     * @return VideoSummaryResponse
     */
    public VideoSummaryResponse toSummaryResponse(Video video, Long scrapId) {
        try {
            var videoDto = new VideoBasicDto(
                    video.getApiVideoId(),
                    video.getTitle(),
                    video.getDescription(),
                    video.getUploadedAt(),
                    video.getThumbnailUrl(),
                    parseLong(video.getViewCount()),
                    parseLong(video.getLikeCount()),
                    parseInt(video.getCommentCount())
            );

            var channelDto = new ChannelBasicDto(
                    video.getChannelId(),
                    video.getChannelName(),
                    video.getChannelThumbnailUrl(),
                    parseLong(video.getSubscriberCount())
            );

            var analysisDto = new VideoSummaryResponse.Analysis(
                    video.getSummation(),
                    parseSentimentDistribution(video.getSentimentDistribution()),
                    parseKeywords(video.getKeywords())
            );

            return new VideoSummaryResponse(videoDto, channelDto, analysisDto);

        } catch (Exception e) {
            log.error("VideoSummaryResponse 변환 실패: videoId={}, error={}",
                    video.getId(), e.getMessage(), e);
            throw new BusinessException(CommonError.DATA_CONVERSION_ERROR);
        }
    }

    /**
     * Video -> DetailVideoDto 변환
     */
    public DetailVideoDto toDetailVideoDto(Video video, Long scrapId) {
        return new DetailVideoDto(
                video.getApiVideoId(),
                video.getTitle(),
                video.getDescription(),
                video.getUploadedAt(),
                video.getThumbnailUrl(),
                parseLong(video.getViewCount()),
                parseLong(video.getLikeCount()),
                parseInt(video.getCommentCount())
        );
    }

    /**
     * Video -> DetailChannelDto 변환
     */
    public DetailChannelDto toDetailChannelDto(Video video, Long favoriteChannelId) {
        return new DetailChannelDto(
                video.getChannelId(),
                video.getChannelName(),
                video.getChannelThumbnailUrl(),
                parseLong(video.getSubscriberCount())
        );
    }

    /**
     * 삭제된 영상용 VideoSummaryResponse 생성
     */
    public VideoSummaryResponse toDeletedVideoResponse(Video video, Long scrapId) {
        var videoDto = new VideoBasicDto(
                video.getApiVideoId(),
                "[삭제된 영상] " + video.getTitle(),
                "이 영상은 삭제되었거나 비공개 처리되었습니다.",
                video.getUploadedAt(),
                video.getThumbnailUrl(),
                0L, 0L, 0
        );

        var channelDto = new ChannelBasicDto(
                video.getChannelId(),
                video.getChannelName(),
                video.getChannelThumbnailUrl(),
                0L
        );

        var analysisDto = new VideoSummaryResponse.Analysis(
                "이 영상은 삭제되었습니다.",
                null,
                List.of()
        );

        return new VideoSummaryResponse(videoDto, channelDto, analysisDto);
    }

    // ==================== Private Helper Methods ====================

    /**
     * SentimentDistribution JSON 파싱
     */
    private VideoSummaryResponse.SentimentDistribution parseSentimentDistribution(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            Map<String, Integer> sentimentMap = objectMapper.readValue(
                    json, new TypeReference<Map<String, Integer>>() {});

            return new VideoSummaryResponse.SentimentDistribution(
                    sentimentMap.getOrDefault("positive", 0),
                    sentimentMap.getOrDefault("negative", 0),
                    sentimentMap.getOrDefault("other", 0)
            );
        } catch (Exception e) {
            log.warn("SentimentDistribution 파싱 실패: json={}", json);
            return null;
        }
    }

    /**
     * Keywords JSON 파싱
     */
    private List<String> parseKeywords(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Keywords 파싱 실패: json={}", json);
            return new ArrayList<>();
        }
    }

    /**
     * String -> Long 안전 변환
     */
    private Long parseLong(String value) {
        try {
            return value != null && !value.trim().isEmpty() ? Long.parseLong(value) : 0L;
        } catch (NumberFormatException e) {
            log.warn("Long 변환 실패: value={}", value);
            return 0L;
        }
    }

    /**
     * String -> Integer 안전 변환
     */
    private Integer parseInt(String value) {
        try {
            return value != null && !value.trim().isEmpty() ? Integer.parseInt(value) : 0;
        } catch (NumberFormatException e) {
            log.warn("Integer 변환 실패: value={}", value);
            return 0;
        }
    }
}