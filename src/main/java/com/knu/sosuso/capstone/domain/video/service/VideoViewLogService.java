package com.knu.sosuso.capstone.domain.video.service;

import com.knu.sosuso.capstone.domain.video.entity.VideoViewLog;
import com.knu.sosuso.capstone.domain.video.repository.VideoViewLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 영상 조회 로그 저장 서비스
 * 여러 Controller에서 호출되므로 독립 서비스로 분리
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class VideoViewLogService {

    private final VideoViewLogRepository viewLogRepository;

    /**
     * 영상 조회 로그 저장
     * 비동기로 처리하여 메인 기능에 영향 없음
     *
     * @param apiVideoId 영상 ID
     * @param userId 사용자 ID (비로그인이면 null)
     */
    @Async
    @Transactional
    public void logVideoView(String apiVideoId, Long userId) {
        try {
            VideoViewLog viewLog = VideoViewLog.builder()
                    .apiVideoId(apiVideoId)
                    .viewedAt(LocalDateTime.now())
                    .userId(userId)
                    .build();

            viewLogRepository.save(viewLog);

            log.debug("영상 조회 로그 저장 완료: apiVideoId={}, userId={}", apiVideoId, userId);

        } catch (Exception e) {
            // 로그 저장 실패는 메인 기능에 영향을 주지 않음
            log.warn("조회 로그 저장 실패: apiVideoId={}, error={}", apiVideoId, e.getMessage());
        }
    }
}