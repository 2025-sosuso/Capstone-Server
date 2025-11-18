package com.knu.sosuso.capstone.domain.detail.dto;

/**
 * 사용자별 영상 상태 (캐시 불가)
 * - 스크랩 여부
 * - 관심 채널 여부
 */
public record UserVideoStateResponse(
        Long scrapId,              // null이면 스크랩 안 함
        Long favoriteChannelId     // null이면 관심 채널 아님
) {
}