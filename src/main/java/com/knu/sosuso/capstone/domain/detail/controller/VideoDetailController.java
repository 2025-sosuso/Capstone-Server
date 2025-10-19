package com.knu.sosuso.capstone.domain.detail.controller;

import com.knu.sosuso.capstone.domain.detail.dto.*;
import com.knu.sosuso.capstone.domain.detail.service.VideoDetailService;
import com.knu.sosuso.capstone.global.ResponseDto;
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

    /**
     * 영상 기본 정보 조회
     * - 영상 정보 (제목, 썸네일, 조회수 등)
     * - 채널 정보 (채널명, 구독자 수 등)
     */
    @GetMapping("/{apiVideoId}/basic")
    public ResponseEntity<ResponseDto<VideoBasicResponse>> getVideoBasic(
            @CookieValue(value = "Authorization", required = false) String token,
            @PathVariable String apiVideoId) {
        try {
            log.info("영상 기본 정보 조회 요청: apiVideoId={}", apiVideoId);

            VideoBasicResponse result = videoDetailService.getVideoBasic(token, apiVideoId);

            log.info("영상 기본 정보 조회 완료: apiVideoId={}", apiVideoId);
            return ResponseEntity.ok(ResponseDto.of(result, "영상 기본 정보 조회 성공"));

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDto.of(e.getMessage()));

        } catch (Exception e) {
            log.error("영상 기본 정보 조회 실패: apiVideoId={}, error={}", apiVideoId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ResponseDto.of("영상 기본 정보 조회 중 오류가 발생했습니다."));
        }
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
        try {
            log.info("영상 분석 정보 조회 요청: apiVideoId={}", apiVideoId);

            VideoAnalysisResponse result = videoDetailService.getVideoAnalysis(apiVideoId);

            log.info("영상 분석 정보 조회 완료: apiVideoId={}", apiVideoId);
            return ResponseEntity.ok(ResponseDto.of(result, "영상 분석 정보 조회 성공"));

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDto.of(e.getMessage()));

        } catch (Exception e) {
            log.error("영상 분석 정보 조회 실패: apiVideoId={}, error={}", apiVideoId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ResponseDto.of("영상 분석 정보 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 전체 댓글 조회
     * - 최대 100개
     */
    @GetMapping("/{apiVideoId}/comments/all")
    public ResponseEntity<ResponseDto<List<DetailCommentDto>>> getVideoComments(
            @PathVariable String apiVideoId) {
        try {
            log.info("전체 댓글 조회 요청: apiVideoId={}", apiVideoId);

            List<DetailCommentDto> result = videoDetailService.getVideoComments(apiVideoId);

            log.info("전체 댓글 조회 완료: apiVideoId={}, 댓글 수={}", apiVideoId, result.size());
            return ResponseEntity.ok(ResponseDto.of(result, "전체 댓글 조회 성공"));

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDto.of(e.getMessage()));

        } catch (Exception e) {
            log.error("전체 댓글 조회 실패: apiVideoId={}, error={}", apiVideoId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ResponseDto.of("전체 댓글 조회 중 오류가 발생했습니다."));
        }
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
        try {
            log.info("AI 분석 결과 조회 요청: apiVideoId={}", apiVideoId);

            AIAnalysisResponse result = videoDetailService.getAIAnalysis(apiVideoId);

            log.info("AI 분석 결과 조회 완료: apiVideoId={}", apiVideoId);
            return ResponseEntity.ok(ResponseDto.of(result, "AI 분석 결과 조회 성공"));

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDto.of(e.getMessage()));

        } catch (Exception e) {
            log.error("AI 분석 결과 조회 실패: apiVideoId={}, error={}", apiVideoId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ResponseDto.of("AI 분석 결과 조회 중 오류가 발생했습니다."));
        }
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

        try {
            log.info("비디오 상세 정보 요청: apiVideoId={}", apiVideoId);

            DetailPageResponse result = videoDetailService.getVideoDetail(token, apiVideoId);

            log.info("비디오 상세 정보 조회 완료: apiVideoId={}", apiVideoId);
            return ResponseEntity.ok(ResponseDto.of(result, "비디오 상세 정보 조회 성공 (Deprecated)"));

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDto.of(e.getMessage()));

        } catch (Exception e) {
            log.error("비디오 상세 정보 조회 실패: apiVideoId={}, error={}", apiVideoId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ResponseDto.of("비디오 상세 정보 조회 중 오류가 발생했습니다."));
        }
    }
}