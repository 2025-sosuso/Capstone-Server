package com.knu.sosuso.capstone.domain.common.dto;

/**
 * 댓글 작성 시간대 공통 DTO
 */
public record CommentHistogram(
        String hour,
        Integer count
) {
}