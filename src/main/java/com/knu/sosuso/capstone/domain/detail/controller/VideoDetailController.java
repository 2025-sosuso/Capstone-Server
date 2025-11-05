package com.knu.sosuso.capstone.domain.detail.controller;

import com.knu.sosuso.capstone.domain.comment.dto.CommentDto;
import com.knu.sosuso.capstone.domain.detail.dto.*;
import com.knu.sosuso.capstone.domain.detail.service.VideoDetailService;
import com.knu.sosuso.capstone.domain.video.service.VideoViewLogService;
import com.knu.sosuso.capstone.global.ResponseDto;
import com.knu.sosuso.capstone.global.security.jwt.JwtUtil;
import com.knu.sosuso.capstone.global.swagger.VideoDetailControllerSwagger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/videos")
public class VideoDetailController implements VideoDetailControllerSwagger {

    private final VideoDetailService videoDetailService;
    private final VideoViewLogService viewLogService;
    private final JwtUtil jwtUtil;

    /**
     * 영상 기본 정보 조회
     * - 영상 정보 (제목, 썸네일, 조회수 등)
     * - 채널 정보 (채널명, 구독자 수 등)
     */
    @GetMapping("/{apiVideoId}/basic")
    public ResponseEntity<ResponseDto<VideoBasicResponse>> getVideoBasic(
            @CookieValue(value = "Authorization", required = false) String token,
            @PathVariable String apiVideoId) {

        log.info("영상 기본 정보 조회 요청: apiVideoId={}", apiVideoId);

        // 조회 로그 저장
        try {
            Long userId = extractUserId(token);
            viewLogService.logVideoView(apiVideoId, userId);
            log.debug("영상 조회 로그 저장: apiVideoId={}", apiVideoId);
        } catch (Exception e) {
            log.warn("조회 로그 저장 실패: {}", e.getMessage());
        }

        VideoBasicResponse result = videoDetailService.getVideoBasic(token, apiVideoId);

        log.info("영상 기본 정보 조회 완료: apiVideoId={}", apiVideoId);
        return ResponseEntity.ok(ResponseDto.of(result, "영상 기본 정보 조회 성공"));
    }

    /**
     * 영상 분석 정보 조회 (백엔드 분석)
     * - 댓글 히스토그램 (시간대별 분포)
     * - 인기 타임스탬프
     * - 좋아요 TOP 5 댓글
     */
    @GetMapping("/{apiVideoId}/analysis")
    public ResponseEntity<ResponseDto<VideoAnalysisResponse>> getVideoAnalysis(
            @PathVariable String apiVideoId) {

        log.info("영상 분석 정보 조회 요청: apiVideoId={}", apiVideoId);

        VideoAnalysisResponse result = videoDetailService.getVideoAnalysis(apiVideoId);

        log.info("영상 분석 정보 조회 완료: apiVideoId={}", apiVideoId);
        return ResponseEntity.ok(ResponseDto.of(result, "영상 분석 정보 조회 성공"));
    }

    /**
     * 전체 댓글 조회
     * - 최대 100개
     */
    @GetMapping("/{apiVideoId}/comments/all")
    public ResponseEntity<ResponseDto<List<CommentDto>>> getVideoComments(
            @PathVariable String apiVideoId) {

        log.info("전체 댓글 조회 요청: apiVideoId={}", apiVideoId);

        List<CommentDto> result = videoDetailService.getVideoComments(apiVideoId);

        log.info("전체 댓글 조회 완료: apiVideoId={}, 댓글 수={}", apiVideoId, result.size());
        return ResponseEntity.ok(ResponseDto.of(result, "전체 댓글 조회 성공"));
    }

    /**
     * AI 분석 결과 조회
     * - AI 요약
     * - 언어 분포
     * - 감정 분포
     * - 키워드
     */
    @GetMapping("/{apiVideoId}/ai")
    public ResponseEntity<ResponseDto<AIAnalysisResponse>> getAIAnalysis(
            @PathVariable String apiVideoId) {

        log.info("AI 분석 결과 조회 요청: apiVideoId={}", apiVideoId);

        AIAnalysisResponse result = videoDetailService.getAIAnalysis(apiVideoId);

        log.info("AI 분석 결과 조회 완료: apiVideoId={}", apiVideoId);
        return ResponseEntity.ok(ResponseDto.of(result, "AI 분석 결과 조회 성공"));
    }

    /**
     * @deprecated 기존 통합 API - 하위 호환성을 위해 유지
     * 프론트엔드 마이그레이션 후 제거 예정
     */
    @Deprecated
    @GetMapping("/{apiVideoId}")
    public ResponseEntity<ResponseDto<DetailPageResponse>> getVideoDetail(
            @CookieValue(value = "Authorization", required = false) String token,
            @PathVariable String apiVideoId) {

        log.warn("Deprecated API 호출: GET /api/videos/{} - 새로운 분리된 API 사용을 권장합니다.", apiVideoId);
        log.info("비디오 상세 정보 요청: apiVideoId={}", apiVideoId);

        DetailPageResponse result = videoDetailService.getVideoDetail(token, apiVideoId);

        log.info("비디오 상세 정보 조회 완료: apiVideoId={}", apiVideoId);
        return ResponseEntity.ok(ResponseDto.of(result, "비디오 상세 정보 조회 성공 (Deprecated)"));
    }

    /**
     * 토큰에서 userId 추출
     */
    private Long extractUserId(String token) {
        if (token == null || !jwtUtil.isValidToken(token)) {
            return null;
        }
        return jwtUtil.getUserId(token);
    }
}