package com.knu.sosuso.capstone.service;

import com.knu.sosuso.capstone.dto.response.CommentApiResponse;
import com.knu.sosuso.capstone.dto.response.SearchUnifiedResponse;
import com.knu.sosuso.capstone.dto.response.VideoApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class TrendingService {

    private final VideoService videoService;
    private final CommentService commentService;

    public TrendingService(VideoService videoService, CommentService commentService) {
        this.videoService = videoService;
        this.commentService = commentService;
    }

    public List<SearchUnifiedResponse> getTrendingVideoWithComments(String categoryId, int maxResults) {
        List<VideoApiResponse.Item> videos = videoService.getTrendingVideos(categoryId, maxResults);
        List<SearchUnifiedResponse> results = new ArrayList<>();

        for (VideoApiResponse.Item video : videos) {
            String videoId = video.id();

            try {
                VideoApiResponse videoDetail = videoService.getVideoInfo(videoId);
                CommentApiResponse comments = commentService.getCommentInfo(videoId);

                results.add(new SearchUnifiedResponse(videoDetail, comments));
            } catch (Exception e) {
                log.warn("영상 처리 중 오류 발생 (videoId: {}) -> 건너뜀", videoId, e);
            }
        }

        return results;
    }
}
