package com.knu.sosuso.capstone.controller;

import com.knu.sosuso.capstone.dto.VideoApiResponse;
import com.knu.sosuso.capstone.service.VideoService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/api/youtube")
public class VideoController {

    private final VideoService apiService;

    public VideoController(VideoService apiService) {
        this.apiService = apiService;
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam("url") String url) {
        if (!apiService.isVideoUrl(url)) {
            return ResponseEntity.badRequest().body("지원되지 않는 유튜브 영상 링크 형식입니다.");
        }

        String videoId = apiService.extractVideoId(url);
        if (videoId == null || videoId.isBlank()) {
            return ResponseEntity.badRequest().body("videoId 추출 실패: URL이 유효하지 않습니다.");
        }

        try {
            VideoApiResponse response = apiService.getVideoInfo(videoId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("YouTube API 호출 중 오류 발생: " + e.getMessage());
        }
    }

}
