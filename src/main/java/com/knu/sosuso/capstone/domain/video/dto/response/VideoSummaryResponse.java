package com.knu.sosuso.capstone.domain.video.dto.response;

import com.knu.sosuso.capstone.domain.common.dto.ChannelBasicDto;
import com.knu.sosuso.capstone.domain.common.dto.VideoBasicDto;

import java.util.List;

/**
 * 영상 요약 정보 응답 DTO
 */
public record VideoSummaryResponse(
        VideoBasicDto video,
        ChannelBasicDto channel,
        Analysis analysis
) {
    public record Analysis(
            String summary,
            SentimentDistribution sentimentDistribution,
            List<String> keywords
    ) {}

    public record SentimentDistribution(
            Integer positive,
            Integer negative,
            Integer other
    ) {}
}