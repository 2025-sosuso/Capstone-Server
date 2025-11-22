package com.knu.sosuso.capstone.domain.detail.dto;

import com.knu.sosuso.capstone.domain.common.dto.ChannelBasicDto;
import com.knu.sosuso.capstone.domain.common.dto.VideoBasicDto;

public record VideoBasicResponse(
        VideoBasicDto video,
        ChannelBasicDto channel
) {
}