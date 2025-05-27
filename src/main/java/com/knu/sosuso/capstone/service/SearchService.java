package com.knu.sosuso.capstone.service;

import com.knu.sosuso.capstone.dto.response.CommentApiResponse;
import com.knu.sosuso.capstone.dto.response.SearchUnifiedResponse;
import com.knu.sosuso.capstone.dto.response.VideoApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);

    private final VideoService videoService;
    private final CommentService commentService;
    private final ChannelService channelService;

    public SearchService(VideoService videoService, CommentService commentService, ChannelService channelService) {
        this.videoService = videoService;
        this.commentService = commentService;
        this.channelService = channelService;
    }

    public Object search(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어는 필수입니다");
        }

        String trimmedQuery = query.trim();
        logger.info("검색 요청: query={}, type={}", trimmedQuery,
                isVideoUrl(trimmedQuery) ? "VIDEO" : "CHANNEL");

        try {
            if (isVideoUrl(trimmedQuery)) {
                return searchVideo(trimmedQuery);
            } else {
                return searchChannels(trimmedQuery);
            }
        } catch (Exception e) {
            logger.error("검색 실패: query={}, error={}", trimmedQuery, e.getMessage(), e);
            throw e;
        }
    }

    public boolean isVideoUrl(String url) {
        return url.contains("youtube.com/watch?v=") || url.contains("youtu.be/");
    }

    private SearchUnifiedResponse searchVideo(String videoUrl) {
        String videoId = videoService.extractVideoId(videoUrl);

        if (videoId == null || videoId.trim().isEmpty()) {
            throw new IllegalArgumentException("유효하지 않은 YouTube URL입니다");
        }

        logger.info("비디오 검색 시작: videoId={}", videoId);

        try {
            CommentApiResponse commentInfo = commentService.getCommentInfo(videoId);
            VideoApiResponse videoInfo = videoService.getVideoInfo(videoId, commentInfo.allComments().size());

            logger.info("비디오 검색 완료: videoId={}, 댓글수={}",
                    videoId, commentInfo.allComments().size());

            return new SearchUnifiedResponse(videoInfo, commentInfo);

        } catch (Exception e) {
            logger.error("비디오 검색 실패: videoId={}, error={}", videoId, e.getMessage());
            throw e;
        }
    }

    private Object searchChannels(String query) {
        logger.info("채널 검색 시작: query={}", query);

        try {
            return channelService.searchChannels(query);
        } catch (Exception e) {
            logger.error("채널 검색 실패: query={}, error={}", query, e.getMessage());
            throw e;
        }
    }
}