package com.knu.sosuso.capstone.dto.response;

import java.util.List;

public record CommentApiResponse(
        List<CommentData> allComments               // 전체 댓글
) {
    public record CommentData(
            String id,
            String authorName,                       // 작성자 이름
            String commentText,                      // 댓글 본문
            int likeCount,                           // 좋아요 수
            String emotion,                          // 감정 분석
            String publishedAt                       // 작성 시각
    ) {
    }
}