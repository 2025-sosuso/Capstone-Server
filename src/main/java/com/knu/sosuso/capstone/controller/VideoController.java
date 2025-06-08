package com.knu.sosuso.capstone.controller;

import com.knu.sosuso.capstone.dto.ResponseDto;
import com.knu.sosuso.capstone.dto.response.comment.CommentResponse;
import com.knu.sosuso.capstone.service.CommentQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
@Slf4j
public class VideoController {

    private final CommentQueryService commentQueryService;

    /**
     * 댓글 조회 (필터링/검색)
     * GET /api/videos/{apiVideoId}/comments?sentiment=NEGATIVE
     * GET /api/videos/{apiVideoId}/comments?q=고양이
     * GET /api/videos/{apiVideoId}/comments?q=고양이&sentiment=NEGATIVE
     */
    @GetMapping("/{apiVideoId}/comments")
    public ResponseEntity<ResponseDto<CommentResponse>> getComments(
            @PathVariable String apiVideoId,
            @RequestParam(required = false) String sentiment,
            @RequestParam(required = false) String q) {

        try {
            // 최소 하나의 파라미터는 있어야 함
            if (sentiment == null && q == null) {
                return ResponseEntity.badRequest()
                        .body(ResponseDto.of("sentiment 또는 q 파라미터 중 하나는 필수입니다."));
            }

            log.info("댓글 조회: apiVideoId={}, sentiment={}, q={}", apiVideoId, sentiment, q);

            CommentResponse result = commentQueryService.getComments(apiVideoId, sentiment, q);

            log.info("댓글 조회 완료: apiVideoId={}, 결과 수={}", apiVideoId, result.results().size());

            String message = buildSuccessMessage(sentiment, q);
            return ResponseEntity.ok(ResponseDto.of(result, message));

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: apiVideoId={}, sentiment={}, q={}, error={}",
                    apiVideoId, sentiment, q, e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDto.of("잘못된 요청: " + e.getMessage()));

        } catch (Exception e) {
            log.error("댓글 조회 실패: apiVideoId={}, sentiment={}, q={}, error={}",
                    apiVideoId, sentiment, q, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ResponseDto.of("댓글 조회 중 오류 발생: " + e.getMessage()));
        }
    }

    /**
     * 성공 메시지 생성
     */
    private String buildSuccessMessage(String sentiment, String q) {
        if (sentiment != null && q != null) {
            return String.format("'%s' 검색 및 %s 감정 필터링이 완료되었습니다.", q, sentiment.toUpperCase());
        } else if (q != null) {
            return String.format("'%s' 검색이 완료되었습니다.", q);
        } else {
            return String.format("%s 감정 댓글 조회가 완료되었습니다.", sentiment.toUpperCase());
        }
    }
}