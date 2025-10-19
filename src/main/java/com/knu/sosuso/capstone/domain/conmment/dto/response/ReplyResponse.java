package com.knu.sosuso.capstone.domain.conmment.dto.response;

import com.knu.sosuso.capstone.domain.conmment.dto.ReplyDto;
import java.util.List;

public record ReplyResponse(
        String apiCommentId,
        List<ReplyDto> replies
) {}