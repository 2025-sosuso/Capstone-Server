package com.knu.sosuso.capstone.service;

import com.knu.sosuso.capstone.config.ApiConfig;
import com.knu.sosuso.capstone.dto.response.VideoApiResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VideoService {  // 실제 YouTube API 요청

    private final ApiConfig config;
    private final RestTemplate restTemplate;

    public VideoService(ApiConfig config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
    }

    public boolean isVideoUrl(String url) {
        return url.contains("youtube.com/watch?v=") || url.contains("youtu.be/");
    }

    public String extractVideoId(String url) {
        if (url == null || url.isBlank()) return null;

        // 유튜브 링크에서 videoId 추출: watch?v= / youtu.be/ / m.youtube.com / embed/
        String pattern = "(?:v=|be/|embed/)([\\w-]{11})";

        Pattern compiled = Pattern.compile(pattern);
        Matcher matcher = compiled.matcher(url);

        if (matcher.find()) {
            return matcher.group(1); // 첫 번째 캡처 그룹 반환
        }

        return null;
    }

    public VideoApiResponse getVideoInfo(String videoId) {
        String apiUrl = UriComponentsBuilder.fromUriString("https://www.googleapis.com/youtube/v3/videos")
                .queryParam("part", "snippet,statistics")
                .queryParam("id", videoId)
                .queryParam("key", config.getKey())
                .queryParam("hl", "ko")
                .toUriString();

        return restTemplate.getForObject(apiUrl, VideoApiResponse.class);
    }

}
