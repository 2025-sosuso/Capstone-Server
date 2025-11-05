package com.knu.sosuso.capstone.domain.comment.dto.response;

import com.knu.sosuso.capstone.domain.comment.dto.ReplyDto;
import java.util.List;

public record ReplyResponse(
        String apiCommentId,
        List<ReplyDto> replies
) {}