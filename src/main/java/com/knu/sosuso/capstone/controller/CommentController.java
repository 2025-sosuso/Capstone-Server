package com.knu.sosuso.capstone.controller;

import com.knu.sosuso.capstone.dto.CommentApiResponse;
import com.knu.sosuso.capstone.service.CommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/youtube")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/comments")
    public ResponseEntity<CommentApiResponse> getComments(@RequestParam("videoId") String videoId) {
        return ResponseEntity.ok(commentService.getCommentInfo(videoId));
    }
}
