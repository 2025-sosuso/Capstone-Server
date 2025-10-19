package com.knu.sosuso.capstone.domain.main;

import com.knu.sosuso.capstone.domain.channel.dto.response.FavoriteChannelListResponse;
import com.knu.sosuso.capstone.domain.channel.dto.response.FavoriteVideoInfoResponse;
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

        try {
            MainPageResponse.FavoriteChannelResponse response =
                    mainPageService.getFavoriteChannelResponse(token);

            log.info("관심 채널 섹션 조회 성공: 채널 수={}",
                    response.favoriteChannelList() != null ? response.favoriteChannelList().size() : 0);

            return ResponseEntity.ok(ResponseDto.of(response, "관심 채널 섹션 조회 완료"));

        } catch (Exception e) {
            log.error("관심 채널 섹션 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ResponseDto.of("관심 채널 섹션 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 메인 페이지 - 인기 급상승 섹션
     * 최신 인기 급상승 영상 3개
     */
    @GetMapping("/trending")
    public ResponseEntity<ResponseDto<List<VideoSummaryResponse>>> getTrending(
            @CookieValue(value = "Authorization", required = false) String token) {

        log.info("메인 페이지 - 인기 급상승 섹션 조회 요청");

        try {
            List<VideoSummaryResponse> response = mainPageService.getTrendingVideos(token);

            log.info("인기 급상승 섹션 조회 성공: 영상 수={}", response.size());
            return ResponseEntity.ok(ResponseDto.of(response, "인기 급상승 섹션 조회 완료"));

        } catch (Exception e) {
            log.error("인기 급상승 섹션 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ResponseDto.of("인기 급상승 섹션 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 메인 페이지 - 스크랩 섹션
     * 사용자가 스크랩한 영상 목록 (최대 3개)
     */
    @GetMapping("/scraps")
    public ResponseEntity<ResponseDto<List<VideoSummaryResponse>>> getScraps(
            @CookieValue(value = "Authorization") String token) {

        log.info("메인 페이지 - 스크랩 섹션 조회 요청");

        try {
            List<VideoSummaryResponse> response = mainPageService.getScrapVideos(token);

            log.info("스크랩 섹션 조회 성공: 영상 수={}", response.size());
            return ResponseEntity.ok(ResponseDto.of(response, "스크랩 섹션 조회 완료"));

        } catch (Exception e) {
            log.error("스크랩 섹션 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ResponseDto.of("스크랩 섹션 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * @deprecated 기존 통합 API - 하위 호환성을 위해 유지
     * 프론트엔드 마이그레이션 후 제거 예정
     */
    @Deprecated
    @GetMapping
    public ResponseEntity<ResponseDto<MainPageResponse>> getMainPageData(
            @CookieValue(value = "Authorization", required = false) String token) {

        log.warn("Deprecated API 호출: GET /api/main - 새로운 분리된 API 사용을 권장합니다.");

        try {
            MainPageResponse response = mainPageService.getMainPageData(token);
            return ResponseEntity.ok(ResponseDto.of(response, "메인 페이지 조회 완료 (Deprecated)"));

        } catch (Exception e) {
            log.error("메인 페이지 데이터 조회 실패: {}", e.getMessage(), e);
            throw e;
        }
    }
}