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
public class CommentController {

    private final CommentQueryService commentQueryService;

    /**
     * 댓글 검색 API (단일 조건만)
     * 사용 예시:
     * GET /api/videos/{apiVideoId}/comments?q=재미있다               # 일반 텍스트 검색
     * GET /api/videos/{apiVideoId}/comments?sentiment=POSITIVE      # 감정별 필터링
     * GET /api/videos/{apiVideoId}/comments?keyword=재미있다         # AI 키워드 검색
     */
    @GetMapping("/{apiVideoId}/comments")
    public ResponseEntity<ResponseDto<CommentResponse>> searchComments(
            @PathVariable String apiVideoId,
            @RequestParam(required = false) String q,           // 일반 텍스트 검색
            @RequestParam(required = false) String keyword,     // AI 키워드 검색
            @RequestParam(required = false) String sentiment) { // 감정 필터링

        try {
            // 정확히 하나의 파라미터만 허용
            int paramCount = (q != null ? 1 : 0) + (keyword != null ? 1 : 0) + (sentiment != null ? 1 : 0);

            if (paramCount == 0) {
                return ResponseEntity.badRequest()
                        .body(ResponseDto.of("검색 조건이 필요합니다. (q, keyword, sentiment 중 하나)"));
            }

            if (paramCount > 1) {
                return ResponseEntity.badRequest()
                        .body(ResponseDto.of("하나의 검색 조건만 사용할 수 있습니다."));
            }

            log.info("댓글 검색: apiVideoId={}, q={}, keyword={}, sentiment={}",
                    apiVideoId, q, keyword, sentiment);

            CommentResponse result = commentQueryService.searchComments(apiVideoId, q, keyword, sentiment);

            String message = buildSuccessMessage(q, keyword, sentiment, result.results().size());
            return ResponseEntity.ok(ResponseDto.of(result, message));

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDto.of(e.getMessage()));

        } catch (Exception e) {
            log.error("댓글 검색 실패: apiVideoId={}, error={}", apiVideoId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ResponseDto.of("댓글 검색 중 오류가 발생했습니다."));
        }
    }

    /**
     * 성공 메시지 생성
     */
    private String buildSuccessMessage(String q, String keyword, String sentiment, int resultCount) {
        String message;

        if (q != null) {
            message = String.format("'%s' 텍스트 검색이 완료되었습니다.", q);
        } else if (keyword != null) {
            message = String.format("'%s' 키워드 검색이 완료되었습니다.", keyword);
        } else {
            message = String.format("%s 감정 댓글 조회가 완료되었습니다.", sentiment.toUpperCase());
        }

        return message + String.format(" (결과: %d개)", resultCount);
    }
}
