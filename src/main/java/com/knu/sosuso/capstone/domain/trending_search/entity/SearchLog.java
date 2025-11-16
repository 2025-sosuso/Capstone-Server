package com.knu.sosuso.capstone.domain.trending_search.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name = "search_log",
        indexes = {
                @Index(name = "idx_created_at", columnList = "created_at")
        })
@EntityListeners(AuditingEntityListener.class)
public class SearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String keyword;

    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected SearchLog() {
    }

    public SearchLog(String keyword) {
        this.keyword = keyword;
    }
}
