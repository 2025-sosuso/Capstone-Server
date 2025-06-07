package com.knu.sosuso.capstone.domain;

import com.knu.sosuso.capstone.domain.value.SentimentType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "comment")
@NoArgsConstructor
public class Comment extends BaseEntity{

    @JoinColumn(name = "video_id")
    @ManyToOne
    private Video video;

    @Column(name = "api_comment_id", nullable = false)
    private String apiCommentId;

    @Column(name = "comment_content", columnDefinition = "TEXT")
    private String commentContent;

    @Column(name = "like_count")
    private Integer likeCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "sentiment_type")
    private SentimentType sentimentType;

    @Column(name = "writer")
    private String writer;

    @Column(name = "written_at")
    private String writtenAt;

    @Builder
    public Comment(Video video, String apiCommentId, String commentContent,
                   Integer likeCount, SentimentType sentimentType, String writer, String writtenAt) {
        this.video = video;
        this.apiCommentId = apiCommentId;
        this.commentContent = commentContent;
        this.likeCount = likeCount;
        this.sentimentType = sentimentType;
        this.writer = writer;
        this.writtenAt = writtenAt;
    }
}
