package com.knu.sosuso.capstone.domain.video.controller;

import com.knu.sosuso.capstone.domain.video.dto.response.VideoSummaryResponse;
import com.knu.sosuso.capstone.domain.video.service.PopularVideoService;
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

    private final PopularVideoService popularVideoService;

    /**
     * 자체 알고리즘 기반 인기 영상 TOP N
     *
     * 우리 서비스 사용자의 검색/조회 + 스크랩 데이터 기반
     * 매 시간마다 업데이트
     */
    @GetMapping
    public ResponseEntity<ResponseDto<List<VideoSummaryResponse>>> getPopularVideos(
            @CookieValue(value = "Authorization", required = false) String token,
            @RequestParam(defaultValue = "10") int maxResults) {

        log.info("인기 영상 조회 요청: maxResults={}", maxResults);

        List<VideoSummaryResponse> result = popularVideoService.getPopularVideos(token, maxResults);

        log.info("인기 영상 조회 완료: 영상 수={}", result.size());
        return ResponseEntity.ok(ResponseDto.of(result, "인기 영상 조회 성공"));
    }
}