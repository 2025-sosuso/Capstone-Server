package com.knu.sosuso.capstone.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.sosuso.capstone.config.ApiConfig;
import com.knu.sosuso.capstone.dto.response.VideoSummaryResponse;
import com.knu.sosuso.capstone.dto.response.detail.DetailPageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TrendingService {
    private static final String YOUTUBE_VIDEOS_API_URL = "https://www.googleapis.com/youtube/v3/videos";

    private final VideoProcessingService videoProcessingService;
    private final ApiConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public List<VideoSummaryResponse> getTrendingVideoWithComments(String token, String categoryType, int maxResults) {
        log.info("인기급상승 영상 조회 시작: categoryType={}, maxResults={}", categoryType, maxResults);

        try {
            String categoryId = getCategoryId(categoryType);
            List<String> videoIds = fetchTrendingVideoIds(categoryId, maxResults);

            if (videoIds.isEmpty()) {
                log.warn("인기급상승 영상이 없습니다: categoryType={}", categoryType);
                return new ArrayList<>();
            }

            List<VideoSummaryResponse> results = new ArrayList<>();

            for (String videoId : videoIds) {
                try {
                    log.debug("비디오 전체 처리 시작: apiVideoId={}", videoId);

                    // 전체 처리 과정: 영상정보 + 댓글수집 + AI분석 + DB저장
                    DetailPageResponse detailResponse = videoProcessingService.processVideoToSearchResult(token, videoId, true);

                    if (detailResponse != null) {
                        // 처리된 결과에서 VideoSummaryResponse로 변환
                        VideoSummaryResponse summaryResponse = convertToVideoSummaryResponse(detailResponse);
                        results.add(summaryResponse);
                        log.debug("비디오 처리 완료: apiVideoId={}", videoId);
                    } else {
                        log.warn("비디오 처리 결과가 null: apiVideoId={}", videoId);
                    }

                } catch (Exception e) {
                    log.error("개별 비디오 처리 실패: apiVideoId={}, error={}", videoId, e.getMessage(), e);
                    // 개별 비디오 실패는 전체를 중단시키지 않고 계속 진행
                }
            }

            log.info("인기급상승 영상 조회 완료: 요청={}, 성공={}", videoIds.size(), results.size());
            return results;

        } catch (Exception e) {
            log.error("인기급상승 영상 조회 실패: categoryType={}, error={}", categoryType, e.getMessage(), e);
            throw new RuntimeException("인기급상승 영상 조회 중 오류 발생", e);
        }
    }

    /**
     * DetailPageResponse를 VideoSummaryResponse로 변환 (전체 처리 완료 후)
     */
    private VideoSummaryResponse convertToVideoSummaryResponse(DetailPageResponse detailResponse) {
        try {
            var video = detailResponse.video();
            var channel = detailResponse.channel();
            var analysis = detailResponse.analysis();

            // 하위 객체 생성
            VideoSummaryResponse.Video videoDto = new VideoSummaryResponse.Video(
                    video.id(),
                    video.title(),
                    video.description(),
                    video.publishedAt(),
                    video.thumbnailUrl(),
                    video.viewCount(),
                    video.likeCount(),
                    video.commentCount()
            );

            VideoSummaryResponse.Channel channelDto = new VideoSummaryResponse.Channel(
                    channel.id(),
                    channel.title(),
                    channel.thumbnailUrl(),
                    channel.subscriberCount()
            );

            VideoSummaryResponse.SentimentDistribution sentimentDto = null;
            if (analysis != null && analysis.sentimentDistribution() != null) {
                var s = analysis.sentimentDistribution();
                sentimentDto = new VideoSummaryResponse.SentimentDistribution(s.positive(), s.negative(), s.other());
            }

            List<String> keywords = (analysis != null && analysis.keywords() != null) ? analysis.keywords() : List.of();
            String summary = (analysis != null) ? analysis.summary() : null;

            VideoSummaryResponse.Analysis analysisDto = new VideoSummaryResponse.Analysis(
                    summary,
                    sentimentDto,
                    keywords
            );

            return new VideoSummaryResponse(videoDto, channelDto, analysisDto);

        } catch (Exception e) {
            log.error("VideoSummaryResponse 변환 실패: error={}", e.getMessage(), e);
            throw new RuntimeException("응답 변환 중 오류 발생", e);
        }
    }

    private String getCategoryId(String categoryType) {
        return switch (categoryType.toLowerCase()) {
            case "latest" -> "0";
            case "music" -> "10";
            case "game" -> "20";
            default -> throw new IllegalArgumentException("지원하지 않는 카테고리입니다: " + categoryType);
        };
    }

    private List<String> fetchTrendingVideoIds(String categoryId, int maxResults) {
        try {
            String apiUrl = buildTrendingApiUrl(categoryId, maxResults);
            log.debug("인기급상승 API 호출: {}", apiUrl);

            String jsonResponse = restTemplate.getForObject(apiUrl, String.class);
            JsonNode rootNode = objectMapper.readTree(jsonResponse);

            List<String> videoIds = new ArrayList<>();
            JsonNode itemsNode = rootNode.path("items");

            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    String videoId = item.path("id").asText();
                    if (!videoId.isEmpty()) {
                        videoIds.add(videoId);
                    }
                }
            }

            log.info("인기급상승 비디오 ID 조회 완료: 개수={}", videoIds.size());
            return videoIds;
        } catch (Exception e) {
            log.error("인기급상승 비디오 ID 조회 실패: categoryId={}, error={}", categoryId, e.getMessage(), e);
            throw new RuntimeException("인기급상승 목록을 가져올 수 없습니다", e);
        }
    }

    private String buildTrendingApiUrl(String categoryId, int maxResults) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(YOUTUBE_VIDEOS_API_URL)
                .queryParam("part", "id")
                .queryParam("chart", "mostPopular")
                .queryParam("regionCode", "KR")
                .queryParam("maxResults", Math.min(maxResults, 30))  // YouTube API 제한
                .queryParam("key", config.getKey());

        builder.queryParam("videoCategoryId", categoryId);

        return builder.build(false).toUriString();
    }

}
