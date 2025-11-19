package com.knu.sosuso.capstone.domain.main;

import com.knu.sosuso.capstone.domain.video.dto.response.VideoSummaryResponse;
import com.knu.sosuso.capstone.global.ResponseDto;
import com.knu.sosuso.capstone.global.swagger.MainPageControllerSwagger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/main")
public class MainPageController implements MainPageControllerSwagger {

    private final MainPageService mainPageService;

    /**
     * 메인 페이지 - 관심 채널 섹션
     * 관심 채널 목록 + 첫 번째 채널의 최신 영상 1개
     */
    @GetMapping("/favorite-channels")
    public ResponseEntity<ResponseDto<MainPageResponse.FavoriteChannelResponse>> getFavoriteChannels(
            @CookieValue(value = "Authorization", required = false) String token) {

        log.info("메인 페이지 - 관심 채널 섹션 조회 요청");

        MainPageResponse.FavoriteChannelResponse response =
                mainPageService.getFavoriteChannelResponse(token);

        log.info("관심 채널 섹션 조회 성공: 채널 수={}",
                response.favoriteChannelList() != null ? response.favoriteChannelList().size() : 0);

        return ResponseEntity.ok(ResponseDto.of(response, "관심 채널 섹션 조회 완료"));
    }

    /**
     * 메인 페이지 - 인기 급상승 섹션
     * 최신 인기 급상승 영상 3개
     */
    @GetMapping("/trending")
    public ResponseEntity<ResponseDto<List<VideoSummaryResponse>>> getTrending(
            @CookieValue(value = "Authorization", required = false) String token) {

        log.info("메인 페이지 - 인기 급상승 섹션 조회 요청");

        List<VideoSummaryResponse> response = mainPageService.getTrendingVideos(token);

        log.info("인기 급상승 섹션 조회 성공: 영상 수={}", response.size());
        return ResponseEntity.ok(ResponseDto.of(response, "인기 급상승 섹션 조회 완료"));
    }

    /**
     * 메인 페이지 - 스크랩 섹션
     * 사용자가 스크랩한 영상 목록 (최대 3개)
     */
    @GetMapping("/scraps")
    public ResponseEntity<ResponseDto<List<VideoSummaryResponse>>> getScraps(
            @CookieValue(value = "Authorization") String token) {

        log.info("메인 페이지 - 스크랩 섹션 조회 요청");

        List<VideoSummaryResponse> response = mainPageService.getScrapVideos(token);

        log.info("스크랩 섹션 조회 성공: 영상 수={}", response.size());
        return ResponseEntity.ok(ResponseDto.of(response, "스크랩 섹션 조회 완료"));
    }
}