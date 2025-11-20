package com.knu.sosuso.capstone.domain.common.dto;

/**
 * 채널 기본 정보 공통 DTO
 */
public record ChannelBasicDto(
        String id,
        String title,
        String thumbnailUrl,
        Long subscriberCount
) {}