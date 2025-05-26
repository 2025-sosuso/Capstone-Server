package com.knu.sosuso.capstone.service;

import com.knu.sosuso.capstone.dto.response.CommentApiResponse;
import com.knu.sosuso.capstone.dto.response.SearchUnifiedResponse;
import com.knu.sosuso.capstone.dto.response.VideoApiResponse;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SearchService {

    private final VideoService videoService;
    private final CommentService commentService;
    private final ChannelService channelService;

    public SearchService(VideoService videoService, CommentService commentService, ChannelService channelService) {
        this.videoService = videoService;
        this.commentService = commentService;
        this.channelService = channelService;
    }

    public Object search(String query) {
        if (videoService.isVideoUrl(query)) {
            String videoId = Optional.ofNullable(videoService.extractVideoId(query))
                    .filter(id -> !id.isBlank())
                    .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 YouTube URL입니다."));

            VideoApiResponse videoInfo = videoService.getVideoInfo(videoId);
            CommentApiResponse commentInfo = commentService.getCommentInfo(videoId);

            return new SearchUnifiedResponse(videoInfo, commentInfo);
        } else {
            return channelService.searchChannels(query);
        }
    }
}
