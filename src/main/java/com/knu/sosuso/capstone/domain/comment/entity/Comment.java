package com.knu.sosuso.capstone.domain.comment.entity;

import com.knu.sosuso.capstone.domain.video.entity.Video;
import com.knu.sosuso.capstone.domain.comment.entity.value.SentimentType;
import com.knu.sosuso.capstone.global.BaseEntity;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "comment")
@Entity
public class Comment extends BaseEntity {

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

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetailSentiment> detailSentiments = new ArrayList<>();

    @Column(name = "writer")
    private String writer;

    @Column(name = "written_at")
    private String writtenAt;

    @Column(name = "has_replies")
    private Boolean hasReplies;
}