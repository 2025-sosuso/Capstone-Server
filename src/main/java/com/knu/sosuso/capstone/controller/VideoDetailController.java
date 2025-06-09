package com.knu.sosuso.capstone.controller;

import com.knu.sosuso.capstone.dto.ResponseDto;
import com.knu.sosuso.capstone.dto.response.detail.DetailPageResponse;
import com.knu.sosuso.capstone.service.VideoProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/videos")
public class VideoDetailController {

    private final VideoProcessingService videoProcessingService;

    @GetMapping("/{apiVideoId}")
    public ResponseEntity<ResponseDto<DetailPageResponse>> getVideoDetail(
            @CookieValue(value = "Authorization", required = false) String token,
            @PathVariable String apiVideoId) {
        try {
            log.info("비디오 상세 정보 요청: apiVideoId={}", apiVideoId);

            DetailPageResponse result = videoProcessingService.processVideoToSearchResult(token, apiVideoId, true);

            log.info("비디오 상세 정보 조회 완료: apiVideoId={}", apiVideoId);
            return ResponseEntity.ok(ResponseDto.of(result, "비디오 상세 정보 조회 성공"));

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