package com.knu.sosuso.capstone.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VideoApiResponse(
        Item[] items,
        PageInfo pageInfo
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            String id,
            Snippet snippet,
            Statistics statistics
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Snippet(
            String publishedAt,
            String channelId,
            String title,
            String description,
            Thumbnails thumbnails,
            String channelTitle,
            String[] tags,
            String categoryId,
            String liveBroadcastContent,
            String defaultAudioLanguage
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Thumbnails(
            @JsonProperty("default") ThumbnailDetail default_,
            ThumbnailDetail medium,
            ThumbnailDetail high,
            ThumbnailDetail standard,
            ThumbnailDetail maxres
    ) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record ThumbnailDetail(
                String url,
                int width,
                int height
        ) {
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Statistics(
            String viewCount,
            String likeCount,
            String commentCount
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PageInfo(
            int totalResults,
            int resultsPerPage
    ) {
    }
}