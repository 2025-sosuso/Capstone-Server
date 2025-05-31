package com.knu.sosuso.capstone.dto.response;

import java.util.List;
import java.util.Map;

public record CommentApiResponse(
        Map<Integer, Integer> hourlyDistribution,   // 24시간 단위 댓글 분포 추가
        List<CommentData> allComments               // 전체 댓글
) {
    public record CommentData(
            String id,                               // YouTube API 댓글 고유 ID
            String authorName,                       // 작성자 이름
            String commentText,                      // 댓글 본문
            int likeCount,                           // 좋아요 수
            String emotion,                          // 감정 분석
            String publishedAt                       // 작성 시각
    ) {
    }
}