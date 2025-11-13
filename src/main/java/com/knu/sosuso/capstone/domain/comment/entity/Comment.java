package com.knu.sosuso.capstone.domain.comment.entity;

import com.knu.sosuso.capstone.domain.comment.converter.DetailSentimentListConverter;
import com.knu.sosuso.capstone.domain.comment.entity.value.DetailSentimentType;
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
@Table(
        name = "comment",
        indexes = {
                @Index(name = "idx_comment_video_id", columnList = "video_id"),
                @Index(name = "idx_comment_video_sentiment", columnList = "video_id, sentiment_type"),
                @Index(name = "idx_comment_video_like", columnList = "video_id, like_count DESC")
        }
)
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

    // JSON 컬럼으로 세부 감정 저장
    @Convert(converter = DetailSentimentListConverter.class)
    @Column(name = "detail_sentiments", columnDefinition = "JSON")
    @Builder.Default
    private List<DetailSentimentType> detailSentiments = new ArrayList<>();

    @Column(name = "writer")
    private String writer;

    @Column(name = "written_at")
    private String writtenAt;

    @Column(name = "has_replies")
    private Boolean hasReplies;
}