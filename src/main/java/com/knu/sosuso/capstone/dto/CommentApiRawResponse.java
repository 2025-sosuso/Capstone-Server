package com.knu.sosuso.capstone.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CommentApiRawResponse(
        CommentThread[] items,
        String nextPageToken
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CommentThread(
            CommentSnippet snippet
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CommentSnippet(
            TopLevelComment topLevelComment
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TopLevelComment(
            CommentDetailsSnippet snippet
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CommentDetailsSnippet(
            String authorDisplayName,
            String authorProfileImageUrl,
            String textDisplay,
            int likeCount,
            String publishedAt
    ) {
    }
}
