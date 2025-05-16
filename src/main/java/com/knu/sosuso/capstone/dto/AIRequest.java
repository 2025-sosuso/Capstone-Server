package com.knu.sosuso.capstone.dto;

import java.util.List;

public record AIRequest(
        List<String> comments  // 전체 댓글 텍스트
) {
}