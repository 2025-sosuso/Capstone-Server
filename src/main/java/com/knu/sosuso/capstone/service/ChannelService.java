package com.knu.sosuso.capstone.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.sosuso.capstone.config.ApiConfig;
import com.knu.sosuso.capstone.dto.ChannelApiResponse;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


@Service
public class ChannelService {

    private final String apiKey;

    public ChannelService(ApiConfig config) {
        this.apiKey = config.getKey();
    }

    public ChannelApiResponse searchChannels(String query) {
        try {
            // 1. 채널 검색
            String searchApiUrl = "https://www.googleapis.com/youtube/v3/search"
                    + "?part=snippet"
                    + "&type=channel"
                    + "&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&maxResults=25"
                    + "&relevanceLanguage=ko"
                    + "&key=" + apiKey;

            URL searchUrl = new URL(searchApiUrl);
            HttpURLConnection searchConnection = (HttpURLConnection) searchUrl.openConnection();
            searchConnection.setRequestMethod("GET");

            BufferedReader searchReader = new BufferedReader(
                    new InputStreamReader(searchConnection.getInputStream()));
            StringBuilder searchResponse = new StringBuilder();
            String line;
            while ((line = searchReader.readLine()) != null) {
                searchResponse.append(line);
            }
            searchReader.close();

            // 2. JSON 파싱
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(searchResponse.toString());
            JsonNode itemsNode = rootNode.path("items");

            List<ChannelApiResponse.ChannelData> results = new ArrayList<>();

            if (itemsNode.isArray()) {
                // 3. 발견된 모든 채널 ID 수집
                List<String> channelIds = new ArrayList<>();
                for (JsonNode item : itemsNode) {
                    JsonNode idNode = item.path("id");
                    String channelId = idNode.path("channelId").asText();
                    if (channelId != null && !channelId.isEmpty()) {
                        channelIds.add(channelId);
                    }
                }

                if (!channelIds.isEmpty()) {
                    // 4. 채널 ID 목록으로 구독자 수 포함한 상세 정보 요청
                    String channelIdsStr = String.join(",", channelIds);
                    String channelsApiUrl = "https://www.googleapis.com/youtube/v3/channels"
                            + "?part=snippet,statistics"
                            + "&id=" + channelIdsStr
                            + "&key=" + apiKey;

                    URL channelsUrl = new URL(channelsApiUrl);
                    HttpURLConnection channelsConnection = (HttpURLConnection) channelsUrl.openConnection();
                    channelsConnection.setRequestMethod("GET");

                    BufferedReader channelsReader = new BufferedReader(
                            new InputStreamReader(channelsConnection.getInputStream()));
                    StringBuilder channelsResponse = new StringBuilder();
                    while ((line = channelsReader.readLine()) != null) {
                        channelsResponse.append(line);
                    }
                    channelsReader.close();

                    // 5. 상세 정보 파싱 및 결과 생성
                    JsonNode channelsRootNode = mapper.readTree(channelsResponse.toString());
                    JsonNode channelsItemsNode = channelsRootNode.path("items");

                    if (channelsItemsNode.isArray()) {
                        for (JsonNode channel : channelsItemsNode) {
                            String channelId = channel.path("id").asText();
                            JsonNode snippetNode = channel.path("snippet");
                            String title = snippetNode.path("title").asText();
                            String description = snippetNode.path("description").asText();

                            JsonNode thumbnailsNode = snippetNode.path("thumbnails");
                            JsonNode mediumNode = thumbnailsNode.path("medium");
                            String thumbnailUrl = mediumNode.path("url").asText();

                            // 구독자 수 정보 가져오기
                            JsonNode statisticsNode = channel.path("statistics");
                            String subscriberCountStr = statisticsNode.path("subscriberCount").asText();

                            results.add(new ChannelApiResponse.ChannelData(
                                    channelId, title, description, thumbnailUrl, subscriberCountStr));
                        }
                    }

                    // 6. 구독자 수 기준으로 정렬 (내림차순)
                    results.sort((a, b) -> {
                        try {
                            long aSubscribers = Long.parseLong(a.subscriberCount());
                            long bSubscribers = Long.parseLong(b.subscriberCount());
                            return Long.compare(bSubscribers, aSubscribers); // 내림차순
                        } catch (NumberFormatException e) {
                            // 숫자로 변환 불가능한 경우 처리
                            return 0;
                        }
                    });
                }
            }

            return new ChannelApiResponse(results);
        } catch (Exception e) {
            System.err.println("Error searching channels: " + e.getMessage());
            return new ChannelApiResponse(List.of());
        }
    }
}