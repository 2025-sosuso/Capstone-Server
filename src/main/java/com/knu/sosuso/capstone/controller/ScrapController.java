package com.knu.sosuso.capstone.controller;

import com.knu.sosuso.capstone.dto.ResponseDto;
import com.knu.sosuso.capstone.dto.request.CreateScrapRequest;
import com.knu.sosuso.capstone.dto.response.CreateScrapResponse;
import com.knu.sosuso.capstone.dto.response.VideoSummaryResponse;
import com.knu.sosuso.capstone.service.ScrapService;
import com.knu.sosuso.capstone.swagger.ScrapControllerSwagger;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/scraps")
@Slf4j
@RestController
public class ScrapController implements ScrapControllerSwagger {

    private final ScrapService scrapService;

    @PostMapping()
    public ResponseDto<CreateScrapResponse> createScrap(
            @CookieValue("Authorization") String token,
            @RequestBody @Valid CreateScrapRequest createScrapRequest
    ) {
        CreateScrapResponse createScrapResponse = scrapService.createScrap(token, createScrapRequest);
        return ResponseDto.of(createScrapResponse, "Successfully created Scrap");
    }

    @DeleteMapping("{id}")
    public ResponseDto<?> cancelScrap(
            @CookieValue("Authorization") String token,
            @PathVariable("id") Long scrapId
    ) {
        scrapService.cancelScrap(token, scrapId);
        return ResponseDto.of("Successfully canceled the scrap.");
    }

    @GetMapping()
    public ResponseEntity<ResponseDto<List<VideoSummaryResponse>>> getScrappedVideos(
            @CookieValue(value = "Authorization") String token) {
        try {
            var result = scrapService.getScrappedVideos(token);

            log.info("스크랩 영상 리스트 조회 완료: 영상 수={}", result.size());

            return ResponseEntity.ok(ResponseDto.of(result, "스크랩 영상 리스트 조회 성공"));
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ResponseDto.of("잘못된 요청: " + e.getMessage()));
        } catch (Exception e) {
            log.error("스크랩 페이지 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ResponseDto.of("스크랩 페이지 조회 실패: " + e.getMessage()));
        }
    }
}