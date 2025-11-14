package com.knu.sosuso.capstone.domain.scrap.service;

import com.knu.sosuso.capstone.domain.scrap.entity.Scrap;
import com.knu.sosuso.capstone.domain.auth.User;
import com.knu.sosuso.capstone.domain.video.entity.Video;
import com.knu.sosuso.capstone.domain.scrap.dto.request.CreateScrapRequest;
import com.knu.sosuso.capstone.domain.scrap.dto.response.CreateScrapResponse;
import com.knu.sosuso.capstone.domain.video.dto.response.VideoSummaryResponse;
import com.knu.sosuso.capstone.global.exception.BusinessException;
import com.knu.sosuso.capstone.global.exception.error.AuthenticationError;
import com.knu.sosuso.capstone.global.exception.error.CommonError;
import com.knu.sosuso.capstone.global.exception.error.ScrapError;
import com.knu.sosuso.capstone.global.exception.error.VideoError;
import com.knu.sosuso.capstone.domain.scrap.repository.ScrapRepository;
import com.knu.sosuso.capstone.domain.auth.UserRepository;
import com.knu.sosuso.capstone.domain.video.repository.VideoRepository;
import com.knu.sosuso.capstone.global.security.jwt.JwtUtil;
import com.knu.sosuso.capstone.global.service.mapper.VideoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 스크랩 관련 비즈니스 로직
 * VideoMapper를 사용하여 중복 변환 로직 제거
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class ScrapService {

    private final ScrapRepository scrapRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final JwtUtil jwtUtil;
    private final VideoMapper videoMapper;

    /**
     * 스크랩 생성
     */
    @Transactional
    public CreateScrapResponse createScrap(String token, CreateScrapRequest createScrapRequest) {
        if (!jwtUtil.isValidToken(token)) {
            throw new BusinessException(AuthenticationError.INVALID_TOKEN);
        }

        Long userId = jwtUtil.getUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(AuthenticationError.USER_NOT_FOUND));

        String apiVideoId = createScrapRequest.apiVideoId();
        Video video = videoRepository.findByApiVideoId(apiVideoId)
                .orElseThrow(() -> new BusinessException(VideoError.VIDEO_NOT_FOUND));

        boolean existsScrap = scrapRepository.existsByUserIdAndApiVideoId(userId, apiVideoId);
        if (existsScrap) {
            throw new BusinessException(ScrapError.SCRAP_ALREADY_EXISTS);
        }

        Scrap scrap = Scrap.builder()
                .user(user)
                .video(video)
                .apiVideoId(apiVideoId)
                .build();

        scrapRepository.save(scrap);

        log.info("스크랩 생성 완료: userId={}, videoId={}, scrapId={}",
                userId, video.getId(), scrap.getId());

        return new CreateScrapResponse(scrap.getId());
    }

    /**
     * 스크랩 취소 (사용자가 직접 취소하는 유일한 경로)
     */
    @Transactional
    public void cancelScrap(String token, Long scrapId) {
        if (!jwtUtil.isValidToken(token)) {
            throw new BusinessException(AuthenticationError.INVALID_TOKEN);
        }

        Scrap scrap = scrapRepository.findById(scrapId)
                .orElseThrow(() -> new BusinessException(ScrapError.SCRAP_NOT_FOUNT));

        Long userId = jwtUtil.getUserId(token);
        if (!scrap.getUser().getId().equals(userId)) {
            throw new BusinessException(ScrapError.FORBIDDEN_SCRAP_DELETE);
        }

        // 영상 정보 로깅 (삭제된 영상도 스크랩 취소 가능)
        Video video = scrap.getVideo();
        log.info("스크랩 취소: userId={}, scrapId={}, videoId={}, deleted={}",
                userId, scrapId, video.getId(), video.isDeleted());

        scrapRepository.deleteById(scrapId);

        log.info("스크랩 삭제 완료: scrapId={}", scrapId);
    }

    /**
     * 스크랩한 영상 목록 조회 (삭제된 영상 처리 포함)
     * VideoMapper를 사용하여 변환
     */
    @Transactional(readOnly = true)
    public List<VideoSummaryResponse> getScrappedVideos(String token) {
        try {
            // 1. 토큰 검증
            if (!jwtUtil.isValidToken(token)) {
                throw new BusinessException(AuthenticationError.INVALID_TOKEN);
            }

            Long userId = jwtUtil.getUserId(token);

            // 2. 사용자의 스크랩 목록 조회 (최신순)
            List<Scrap> scraps = scrapRepository.findByUserIdOrderByCreatedAtDesc(userId);

            if (scraps.isEmpty()) {
                log.info("스크랩된 영상이 없습니다: userId={}", userId);
                return new ArrayList<>();
            }

            log.info("스크랩 목록 조회 완료: userId={}, 스크랩 수={}", userId, scraps.size());

            // 3. 각 스크랩의 비디오 정보를 VideoSummaryResponse로 변환
            List<VideoSummaryResponse> results = new ArrayList<>();

            for (Scrap scrap : scraps) {
                try {
                    Video video = scrap.getVideo();

                    // VideoMapper 사용으로 중복 코드 제거
                    if (video.isDeleted()) {
                        log.info("삭제된 영상 발견: videoId={}, apiVideoId={}, scrapId={}",
                                video.getId(), video.getApiVideoId(), scrap.getId());
                        results.add(videoMapper.toDeletedVideoResponse(video, scrap.getId()));
                    } else {
                        results.add(videoMapper.toSummaryResponse(video, scrap.getId()));
                    }

                    log.debug("스크랩 영상 변환 완료: apiVideoId={}, title={}, scrapId={}",
                            video.getApiVideoId(), video.getTitle(), scrap.getId());

                } catch (Exception e) {
                    log.error("개별 스크랩 영상 처리 실패: scrapId={}, error={}",
                            scrap.getId(), e.getMessage(), e);
                    // 개별 실패는 전체를 중단시키지 않고 계속 진행
                }
            }

            log.info("스크랩 영상 조회 완료: 요청={}, 성공={}", scraps.size(), results.size());
            return results;

        } catch (BusinessException e) {
            log.error("스크랩 영상 조회 비즈니스 오류: error={}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("스크랩 영상 조회 실패: error={}", e.getMessage(), e);
            throw new BusinessException(CommonError.INTERNAL_SERVER_ERROR);
        }
    }
}