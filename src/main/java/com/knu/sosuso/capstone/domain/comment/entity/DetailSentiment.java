package com.knu.sosuso.capstone.domain.comment.entity;

import com.knu.sosuso.capstone.domain.comment.entity.value.DetailSentimentType;
import com.knu.sosuso.capstone.global.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "detail_sentiment")
@Entity
public class DetailSentiment extends BaseEntity {

    @JoinColumn(name = "comment_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Comment comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "detail_sentiment")
    private DetailSentimentType detailSentimentType;
}
