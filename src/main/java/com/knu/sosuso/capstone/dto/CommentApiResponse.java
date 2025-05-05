package com.knu.sosuso.capstone.dto;

import java.util.List;

public record CommentApiResponse(
        List<CommentData> allComments               // 전체 댓글
) {
    public record CommentData(
            String authorName,                       // 작성자 이름
            String commentText,                      // 댓글 본문
            int likeCount,                           // 좋아요 수
            String publishedAt                       // 작성 시각
    ) {
    }
}