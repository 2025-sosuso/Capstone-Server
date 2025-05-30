package com.knu.sosuso.capstone.domain;

import com.knu.sosuso.capstone.domain.enums.Emotion;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "comment")
@NoArgsConstructor
public class Comment extends BaseEntity{

    @Column(name = "video_id", nullable = false)
    private String videoId;

    @Column(name = "comment_id", nullable = false)
    private String CommentId;

    @Column(name = "comment_content", columnDefinition = "TEXT")
    private String commentContent;

    @Column(name = "like_count")
    private Integer likeCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "emotion")
    private Emotion emotion;

    @Column(name = "writer")
    private String writer;

    @Column(name = "written_at")
    private String writtenAt;

    @Builder
    public Comment(String videoId, String CommentId, String commentContent,
                   Integer likeCount, Emotion emotion, String writer, String writtenAt) {
        this.videoId = videoId;
        this.CommentId = CommentId;
        this.commentContent = commentContent;
        this.likeCount = likeCount;
        this.emotion = emotion;
        this.writer = writer;
        this.writtenAt = writtenAt;
    }
}
