package com.knu.sosuso.capstone.service;

import com.knu.sosuso.capstone.dto.response.CommentApiResponse;
import com.knu.sosuso.capstone.dto.response.SearchUnifiedResponse;
import com.knu.sosuso.capstone.dto.response.VideoApiResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TrendingService {

    private final VideoService videoService;
    private final CommentService commentService;

    public TrendingService(VideoService videoService, CommentService commentService) {
        this.videoService = videoService;
        this.commentService = commentService;
    }

    public List<SearchUnifiedResponse> getTrendingVideoWithComment(int maxResults) {
        List<VideoApiResponse.Item> videos = videoService.getTrendingVideos(maxResults);
        List<SearchUnifiedResponse> results = new ArrayList<>();

        for (VideoApiResponse.Item video : videos) {
            String videoId = video.id();
            VideoApiResponse singleVideo = videoService.getVideoInfo(videoId);
            CommentApiResponse comments = commentService.getCommentInfo(videoId);
            results.add(new SearchUnifiedResponse(singleVideo, comments));
        }

        return results;
    }
}
