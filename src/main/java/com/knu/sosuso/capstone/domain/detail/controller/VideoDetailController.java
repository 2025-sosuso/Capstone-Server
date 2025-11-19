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
     * 사용자별 영상 상태 조회
     * - 스크랩 여부
     * - 관심 채널 여부
     *
     * 공통 영상 정보는 /basic에서 조회하고,
     * 사용자 상태는 이 API로 별도 조회하여 프론트에서 조합
     */
    @GetMapping("/{apiVideoId}/user-state")
    public ResponseEntity<ResponseDto<UserVideoStateResponse>> getUserVideoState(
            @CookieValue(value = "Authorization", required = false) String token,
            @PathVariable String apiVideoId) {

        log.info("👤 사용자 영상 상태 조회 요청: apiVideoId={}", apiVideoId);

        UserVideoStateResponse result = videoDetailService.getUserVideoState(token, apiVideoId);

        log.info("👤 사용자 영상 상태 조회 완료: apiVideoId={}", apiVideoId);
        return ResponseEntity.ok(ResponseDto.of(result, "사용자 영상 상태 조회 성공"));
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
     * 토큰에서 userId 추출
     */
    private Long extractUserId(String token) {
        if (token == null || !jwtUtil.isValidToken(token)) {
            return null;
        }
        return jwtUtil.getUserId(token);
    }
}