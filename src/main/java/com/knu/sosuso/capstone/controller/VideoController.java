package com.knu.sosuso.capstone.controller;

import com.knu.sosuso.capstone.dto.ResponseDto;
import com.knu.sosuso.capstone.dto.response.comment.CommentSearchResponse;
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
     * 특정 비디오의 댓글 검색
     */
    @GetMapping("/{videoId}/comments/search")
    public ResponseEntity<ResponseDto<CommentSearchResponse>> searchCommentsInVideo(
            @PathVariable Long videoId, @RequestParam String query) {

        try {
            log.info("비디오 댓글 검색: videoId={}, query={}", videoId, query);

            CommentSearchResponse result = commentQueryService.searchComments(videoId, query);

            log.info("댓글 검색 완료: videoId={}, 검색된 댓글 수={}", videoId, result.results().size());

            return ResponseEntity.ok(ResponseDto.of(result, "댓글 검색이 완료되었습니다."));
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: videoId={}, query={}, error={}", videoId, query, e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDto.of("잘못된 요청: " + e.getMessage()));
        } catch (Exception e) {
            log.error("댓글 검색 실패: videoId={}, query={}, error={}", videoId, query, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ResponseDto.of("댓글 검색 중 오류 발생: " + e.getMessage()));
        }
    }
}
