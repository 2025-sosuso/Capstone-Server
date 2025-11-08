package com.knu.sosuso.capstone.domain.comment.entity.value;

import java.util.List;

public record CommentSentimentDetail(
        String apiCommentId,
        String content,
        SentimentType sentimentType,
        List<DetailSentimentType> detailSentimentTypes
) {}