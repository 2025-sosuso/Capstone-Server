package com.knu.sosuso.capstone.controller;

import com.knu.sosuso.capstone.dto.ResponseDto;
import com.knu.sosuso.capstone.dto.response.MainPageResponse;
import com.knu.sosuso.capstone.service.FavoriteChannelService;
import com.knu.sosuso.capstone.service.MainPageService;
import com.knu.sosuso.capstone.service.ScrapService;
import com.knu.sosuso.capstone.service.TrendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/main")
public class MainPageController {

    private final MainPageService mainPageService;

    @GetMapping()
    public ResponseEntity<ResponseDto<MainPageResponse>> getMainPageData(
            @CookieValue(value = "Authorization", required = false) String token) {

        log.info("메인 페이지 데이터 조회 요청 수신");

        try {
            MainPageResponse response = mainPageService.getMainPageData(token);

            log.info("메인 페이지 데이터 조회 성공");
            return ResponseEntity.ok(ResponseDto.of(response, "메인 페이지 조회 완료"));

        } catch (Exception e) {
            log.error("메인 페이지 데이터 조회 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

}
