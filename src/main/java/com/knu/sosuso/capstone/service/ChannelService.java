package com.knu.sosuso.capstone.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.sosuso.capstone.config.ApiConfig;
import com.knu.sosuso.capstone.dto.response.search.ChannelSearchResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
public class ChannelService {
    private static final String YOUTUBE_SEARCH_API_URL = "https://www.googleapis.com/youtube/v3/search";
    private static final String YOUTUBE_CHANNELS_API_URL = "https://www.googleapis.com/youtube/v3/channels";

    private final ApiConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final UserDataService userDataService;


    public ChannelSearchResponse searchChannels(String token, String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어는 필수입니다");
        }

        try {
            log.info("채널 검색 시작: query={}", query);

            // 1. 채널 검색
            String searchResponse = searchChannelsByQuery(query.trim());

            // 2. 채널 ID 수집
            List<String> channelIds = extractChannelIds(searchResponse);

            if (channelIds.isEmpty()) {
                log.info("검색된 채널이 없음: query={}", query);
                return new ChannelSearchResponse(List.of());
            }

            // 3. 채널 상세 정보 조회
            String channelsResponse = getChannelsDetails(channelIds);

            // 4. 결과 변환 및 정렬
            List<ChannelSearchResponse.ChannelDto> results = parseChannelsResponse(token, channelsResponse);

            log.info("채널 검색 완료: query={}, resultCount={}", query, results.size());
            return new ChannelSearchResponse(results);

        } catch (HttpClientErrorException.Forbidden e) {
            log.warn("YouTube API 접근 금지: query={}", query);
            throw new IllegalStateException("YouTube API에 접근할 수 없습니다", e);

        } catch (RestClientException e) {
            log.error("YouTube API 호출 실패: query={}, error={}", query, e.getMessage(), e);
            throw new RuntimeException("채널 검색을 수행할 수 없습니다", e);

        } catch (Exception e) {
            log.error("채널 검색 실패: query={}, error={}", query, e.getMessage(), e);
            throw new RuntimeException("채널 검색 중 오류 발생", e);
        }
    }

    private String searchChannelsByQuery(String query) {
        String apiUrl = UriComponentsBuilder.fromUriString(YOUTUBE_SEARCH_API_URL)
                .queryParam("part", "snippet")
                .queryParam("type", "channel")
                .queryParam("q", query)
                .queryParam("maxResults", 25)
                .queryParam("relevanceLanguage", "ko")
                .queryParam("key", config.getKey())
                .build(false)
                .toUriString();

        return restTemplate.getForObject(apiUrl, String.class);
    }

    private List<String> extractChannelIds(String searchResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(searchResponse);
            JsonNode itemsNode = rootNode.path("items");

            List<String> channelIds = new ArrayList<>();

            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    JsonNode idNode = item.path("id");
                    String channelId = idNode.path("channelId").asText();
                    if (channelId != null && !channelId.trim().isEmpty()) {
                        channelIds.add(channelId);
                    }
                }
            }

            return channelIds;
        } catch (Exception e) {
            log.error("채널 ID 추출 실패: {}", e.getMessage(), e);
            return List.of();
        }
    }


    private String getChannelsDetails(List<String> channelIds) {
        String channelIdsStr = String.join(",", channelIds);

        String apiUrl = UriComponentsBuilder.fromUriString(YOUTUBE_CHANNELS_API_URL)
                .queryParam("part", "snippet,statistics")
                .queryParam("id", channelIdsStr)
                .queryParam("key", config.getKey())
                .build(false)
                .toUriString();

        return restTemplate.getForObject(apiUrl, String.class);
    }

    private List<ChannelSearchResponse.ChannelDto> parseChannelsResponse(String token, String channelsResponse) {
        try {
            JsonNode channelsRootNode = objectMapper.readTree(channelsResponse);
            JsonNode channelsItemsNode = channelsRootNode.path("items");

            List<ChannelSearchResponse.ChannelDto> results = new ArrayList<>();

            if (channelsItemsNode.isArray()) {
                for (JsonNode channel : channelsItemsNode) {
                    String channelId = channel.path("id").asText();
                    JsonNode snippetNode = channel.path("snippet");
                    String title = snippetNode.path("title").asText();
                    String handle = snippetNode.path("customUrl").asText();
                    String description = snippetNode.path("description").asText();

                    // 썸네일 URL 추출
                    String thumbnailUrl = extractThumbnailUrl(snippetNode.path("thumbnails"));

                    // 구독자 수 정보 가져오기
                    JsonNode statisticsNode = channel.path("statistics");
                    String subscriberCountStr = statisticsNode.path("subscriberCount").asText();
                    Long subscriberCount = parseLong(subscriberCountStr);

                    Long favoriteChannelId = userDataService.getUserFavoriteChannelId(token, channelId);

                    results.add(new ChannelSearchResponse.ChannelDto(
                            channelId, title, handle, description, thumbnailUrl, subscriberCount, favoriteChannelId));
                }
            }

            // 구독자 수 기준으로 정렬 (내림차순)
            results.sort((a, b) -> Long.compare(b.subscriberCount(), a.subscriberCount()));

            return results;
        } catch (Exception e) {
            log.error("채널 응답 파싱 실패: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private String extractThumbnailUrl(JsonNode thumbnailsNode) {
        // medium 썸네일 우선, 없으면 default
        JsonNode mediumNode = thumbnailsNode.path("medium");
        if (!mediumNode.isMissingNode()) {
            String url = mediumNode.path("url").asText();
            if (!url.isEmpty()) {
                return url;
            }
        }

        JsonNode defaultNode = thumbnailsNode.path("default");
        if (!defaultNode.isMissingNode()) {
            return defaultNode.path("url").asText();
        }

        return "";
    }

    private Long parseLong(String value) {
        try {
            return value != null && !value.isEmpty() ? Long.parseLong(value) : 0L;
        } catch (NumberFormatException e) {
            log.warn("구독자 수 Long 변환 실패: {}", value);
            return 0L;
        }
    }
}