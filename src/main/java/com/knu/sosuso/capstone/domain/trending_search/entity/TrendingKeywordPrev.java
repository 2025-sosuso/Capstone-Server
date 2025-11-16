package com.knu.sosuso.capstone.domain.trending_search.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trending_keyword_prev")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendingKeywordPrev {

    @Id
    @Column(name = "keyword", length = 100)
    private String keyword;

    @Column(name = "count", nullable = false)
    private Long count;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}
