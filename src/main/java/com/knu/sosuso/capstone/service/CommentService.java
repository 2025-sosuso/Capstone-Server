package com.knu.sosuso.capstone.service;

import com.knu.sosuso.capstone.config.ApiConfig;
import com.knu.sosuso.capstone.dto.CommentApiRawResponse;
import com.knu.sosuso.capstone.dto.CommentApiResponse;
import com.knu.sosuso.capstone.dto.CommentApiResponse.CommentData;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class CommentService {

    private final RestTemplate restTemplate;
    private final String apiKey;

    public CommentService(ApiConfig config, RestTemplate restTemplate) {
        this.apiKey = config.getKey();
        this.restTemplate = restTemplate;
    }

    public CommentApiResponse getCommentInfo(String videoId) {

        List<CommentData> allComments = new ArrayList<>();
        String pageToken = null;

        do {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("https://www.googleapis.com/youtube/v3/commentThreads")
                    .queryParam("part", "snippet")
                    .queryParam("maxResults", 100)
                    .queryParam("textFormat", "plainText")
                    .queryParam("videoId", videoId)
                    .queryParam("key", apiKey);

            // 🔥 null 또는 빈 문자열일 경우 pageToken 추가하지 않음
            if (pageToken != null && !pageToken.isBlank()) {
                builder.queryParam("pageToken", pageToken);
            }

            String apiUrl = builder.build(false).toUriString();

            CommentApiRawResponse raw = restTemplate.getForObject(apiUrl, CommentApiRawResponse.class);

            if (raw == null || raw.items() == null) break;

            Arrays.stream(raw.items())
                    .map(item -> {
                        var snippet = item.snippet().topLevelComment().snippet();
                        return new CommentData(
                                snippet.authorDisplayName(),
                                snippet.textDisplay(),
                                snippet.likeCount(),
                                snippet.publishedAt()
                        );
                    })
                    .forEach(allComments::add);

            pageToken = raw.nextPageToken(); // 다음 페이지 토큰 갱신

        } while (pageToken != null && !pageToken.trim().isEmpty());

        return new CommentApiResponse(allComments);
    }
}