package com.knu.sosuso.capstone.domain.video.controller;

import com.knu.sosuso.capstone.domain.video.dto.response.VideoSummaryResponse;
import com.knu.sosuso.capstone.domain.video.service.TrendingService;
import com.knu.sosuso.capstone.global.ResponseDto;
import com.knu.sosuso.capstone.global.swagger.TrendingControllerSwagger;
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

        log.info("인기급상승 영상 조회 요청: categoryType={}, maxResults={}", categoryType, maxResults);

        List<VideoSummaryResponse> result = trendingService.getTrendingVideoWithComments(token, categoryType, maxResults);

        log.info("인기급상승 영상 조회 완료: 영상 수={}", result.size());
        return ResponseEntity.ok(ResponseDto.of(result, "인기급상승 영상 조회 성공"));
    }
}