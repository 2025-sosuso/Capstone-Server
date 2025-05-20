package com.knu.sosuso.capstone.controller;

import com.knu.sosuso.capstone.service.TrendingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trending")
public class TrendingController {

    private final TrendingService trendingService;

    public TrendingController(TrendingService trendingService) {
        this.trendingService = trendingService;
    }

    @GetMapping("/category")
    public ResponseEntity<?> getByCategory(
            @RequestParam(defaultValue = "latest") String type,
            @RequestParam(defaultValue = "5") int count) {
        try {
            String categoryId = switch (type.toLowerCase()) {
                case "latest" -> "0";
                case "music" -> "10";
                case "game" -> "20";
                case "movie" -> "30";
                default -> throw new IllegalArgumentException("지원하지 않는 카테고리입니다: " + type);
            };

            var result = trendingService.getTrendingVideoWithComments(categoryId, count);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("카테고리별 조회 실패: " + e.getMessage());
        }
    }
}
