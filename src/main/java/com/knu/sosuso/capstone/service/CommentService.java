package com.knu.sosuso.capstone.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.sosuso.capstone.config.ApiConfig;
import com.knu.sosuso.capstone.dto.response.CommentApiResponse;
import com.knu.sosuso.capstone.dto.response.CommentApiResponse.CommentData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Service
public class CommentService {

    private static final Logger logger = LoggerFactory.getLogger(CommentService.class);
    private static final String YOUTUBE_COMMENT_API_URL = "https://www.googleapis.com/youtube/v3/commentThreads";
    private static final int MAX_RESULTS_PER_REQUEST = 100;

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    public CommentService(ApiConfig config, RestTemplate restTemplate) {
        this.apiKey = config.getKey();
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public CommentApiResponse getCommentInfo(String videoId) {
        if (videoId == null || videoId.trim().isEmpty()) {
            throw new IllegalArgumentException("비디오 ID는 필수입니다");
        }

        try {
            logger.info("댓글 정보 조회 시작: videoId={}", videoId);

            List<CommentData> allComments = fetchAllComments(videoId.trim());
            allComments.sort(Comparator.comparingInt(CommentData::likeCount).reversed());

            logger.info("댓글 정보 조회 완료: videoId={}, 댓글 수={}", videoId, allComments.size());
            return new CommentApiResponse(allComments);

        } catch (HttpClientErrorException.Forbidden e) {
            logger.warn("댓글 접근 금지: videoId={}", videoId);
            // 빈 댓글 리스트 반환 (댓글 비활성화는 정상적인 상황)
            return new CommentApiResponse(new ArrayList<>());

        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("비디오를 찾을 수 없음: videoId={}", videoId);
            throw new IllegalArgumentException("존재하지 않는 비디오입니다", e);

        } catch (RestClientException e) {
            logger.error("YouTube API 호출 실패: videoId={}, error={}", videoId, e.getMessage(), e);
            throw new IllegalStateException("댓글 정보를 가져올 수 없습니다", e);

        } catch (Exception e) {
            logger.error("댓글 정보 조회 실패: videoId={}, error={}", videoId, e.getMessage(), e);
            throw new RuntimeException("댓글 정보 조회 중 오류 발생", e);
        }
    }

    private List<CommentData> fetchAllComments(String videoId) {
        List<CommentData> allComments = new ArrayList<>();
        String pageToken = null;
        int pageCount = 0;

        do {
            String apiUrl = buildApiUrl(videoId, pageToken);
            String jsonResponse = callYouTubeApi(apiUrl);

            try {
                JsonNode rootNode = objectMapper.readTree(jsonResponse);
                JsonNode itemsNode = rootNode.path("items");

                if (!itemsNode.isArray() || itemsNode.isEmpty()) {
                    break;
                }
                List<CommentData> pageComments = parseCommentsFromJson(itemsNode);
                allComments.addAll(pageComments);

                pageToken = rootNode.path("nextPageToken").asText();
                if (pageToken.isEmpty()) {
                    pageToken = null;
                }

                pageCount++;

                if (pageCount % 10 == 0) {
                    logger.info("댓글 수집 진행: videoId={}, 페이지={}, 누적={}", videoId, pageCount, allComments.size());
                }
            } catch (Exception e) {
                logger.error("JSON 파싱 실패: videoId={}, error={}", videoId, e.getMessage(), e);
                break;
            }

        } while (isValidPageToken(pageToken));

        return allComments;
    }

    private String buildApiUrl(String videoId, String pageToken) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(YOUTUBE_COMMENT_API_URL)
                .queryParam("part", "snippet")
                .queryParam("maxResults", MAX_RESULTS_PER_REQUEST)
                .queryParam("textFormat", "plainText")
                .queryParam("videoId", videoId)
                .queryParam("key", apiKey);

        if (isValidPageToken(pageToken)) {
            builder.queryParam("pageToken", pageToken);
        }

        return builder.build(false).toUriString();
    }

    private String callYouTubeApi(String apiUrl) {
        return restTemplate.getForObject(apiUrl, String.class);
    }

    private List<CommentData> parseCommentsFromJson(JsonNode itemsNode) {
        List<CommentData> comments = new ArrayList<>();

        for (JsonNode item : itemsNode) {
            CommentData commentData = extractCommentData(item);
            if (commentData != null) {
                comments.add(commentData);
            }
        }

        return comments;
    }


    private CommentData extractCommentData(JsonNode item) {
        try {
            JsonNode snippet = item.path("snippet");
            if (snippet.isMissingNode()) {
                return null;
            }

            JsonNode topLevelComment = snippet.path("topLevelComment");
            if (topLevelComment.isMissingNode()) {
                return null;
            }

            JsonNode commentSnippet = topLevelComment.path("snippet");
            if (commentSnippet.isMissingNode()) {
                return null;
            }

            String authorName = commentSnippet.path("authorDisplayName").asText();
            String commentText = commentSnippet.path("textDisplay").asText();
            int likeCount = commentSnippet.path("likeCount").asInt(0);
            String publishedAt = commentSnippet.path("publishedAt").asText();

            return new CommentData(authorName, commentText, likeCount, publishedAt);

        } catch (Exception e) {
            logger.warn("댓글 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    private boolean isValidPageToken(String pageToken) {
        return Optional.ofNullable(pageToken)
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .isPresent();
    }
}