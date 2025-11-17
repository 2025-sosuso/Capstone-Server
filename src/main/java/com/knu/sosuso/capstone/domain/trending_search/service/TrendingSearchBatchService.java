package com.knu.sosuso.capstone.domain.trending_search.service;

import com.knu.sosuso.capstone.domain.trending_search.dto.KeywordCount;
import com.knu.sosuso.capstone.domain.trending_search.entity.TrendingKeyword;
import com.knu.sosuso.capstone.domain.trending_search.entity.TrendingKeywordPrev;
import com.knu.sosuso.capstone.domain.trending_search.repository.TrendingKeywordPrevRepository;
import com.knu.sosuso.capstone.domain.trending_search.repository.TrendingKeywordRepository;
import com.knu.sosuso.capstone.domain.trending_search.repository.SearchLogRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TrendingSearchBatchService {

    private final SearchLogRepository searchLogRepository;
    private final TrendingKeywordRepository trendingKeywordRepository;
    private final TrendingKeywordPrevRepository trendingKeywordPrevRepository;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    @Scheduled(cron = "0 */10 * * * *")
    public synchronized void updateTrendingKeywords() {

        log.info("인기 검색어 배치 실행 시작");

        List<TrendingKeyword> currentAll = trendingKeywordRepository.findAll();

        trendingKeywordPrevRepository.deleteAll();
        em.flush();
        em.clear();

        if (!currentAll.isEmpty()) {
            for (TrendingKeyword keyword : currentAll) {
                TrendingKeywordPrev prev = TrendingKeywordPrev.builder()
                        .keyword(keyword.getKeyword())
                        .count(keyword.getCount())
                        .updatedAt(keyword.getUpdatedAt())
                        .build();
                trendingKeywordPrevRepository.save(prev);
            }
            em.flush();
        }

        trendingKeywordRepository.deleteAll();
        em.flush();
        em.clear();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(4);

        List<KeywordCount> topKeywords = searchLogRepository.findTopKeywordsSince(oneHourAgo, PageRequest.of(0, 10));

        for (KeywordCount count : topKeywords) {
            TrendingKeyword trending = TrendingKeyword.builder()
                    .keyword(count.keyword())
                    .count(count.count())
                    .updatedAt(now)
                    .build();
            trendingKeywordRepository.save(trending);
        }

        log.info("인기 검색어 배치 완료");
    }
}

