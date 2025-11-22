package com.knu.sosuso.capstone.domain.detail.dto;


import com.knu.sosuso.capstone.domain.comment.dto.CommentDto;
import com.knu.sosuso.capstone.domain.common.dto.ChannelBasicDto;
import com.knu.sosuso.capstone.domain.common.dto.VideoBasicDto;

import java.util.List;

public record DetailPageResponse(
        VideoBasicDto video,
        ChannelBasicDto channel,
        DetailAnalysisDto analysis,
        List<CommentDto> comments
) {
}
