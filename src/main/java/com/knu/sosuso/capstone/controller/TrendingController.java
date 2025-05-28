package com.knu.sosuso.capstone.controller;

import com.knu.sosuso.capstone.dto.ResponseDto;
import com.knu.sosuso.capstone.service.TrendingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/api/trending")
public class TrendingController {

    private final TrendingService trendingService;

    public TrendingController(TrendingService trendingService) {
        this.trendingService = trendingService;
    }

    @GetMapping("/category")
    public ResponseEntity<?> getByCategory(
            @RequestParam(defaultValue = "latest") String categoryType,
            @RequestParam(defaultValue = "KR") String regionCode,
            @RequestParam(defaultValue = "5") int maxResults) {
        try {
            log.info("인기급상승 영상 조회 요청: categoryType={}, regionCode={}, maxResults={}", categoryType, regionCode, maxResults);

            var result = trendingService.getTrendingVideoWithComments(categoryType, regionCode, maxResults);

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
