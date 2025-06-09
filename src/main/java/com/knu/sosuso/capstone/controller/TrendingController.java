package com.knu.sosuso.capstone.controller;

import com.knu.sosuso.capstone.dto.ResponseDto;
import com.knu.sosuso.capstone.dto.response.VideoSummaryResponse;
import com.knu.sosuso.capstone.service.TrendingService;
import com.knu.sosuso.capstone.swagger.TrendingControllerSwagger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/trending")
public class TrendingController implements TrendingControllerSwagger {

    private final TrendingService trendingService;

    @GetMapping("/category")
    public ResponseEntity<ResponseDto<List<VideoSummaryResponse>>> getByCategory(
            @CookieValue(value = "Authorization", required = false) String token,
            @RequestParam(defaultValue = "latest") String categoryType,
            @RequestParam(defaultValue = "5") int maxResults) {
        try {
            log.info("인기급상승 영상 조회 요청: categoryType={}, maxResults={}", categoryType, maxResults);

            var result = trendingService.getTrendingVideoWithComments(token, categoryType, maxResults);

            log.info("인기급상승 영상 조회 완료: 영상 수={}", result.size());
            return ResponseEntity.ok(ResponseDto.of(result, "인기급상승 영상 조회 성공"));

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDto.of("잘못된 요청: " + e.getMessage()));

        } catch (Exception e) {
            log.error("인기급상승 영상 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ResponseDto.of("인기급상승 영상 조회 실패: " + e.getMessage()));
        }
    }
}
