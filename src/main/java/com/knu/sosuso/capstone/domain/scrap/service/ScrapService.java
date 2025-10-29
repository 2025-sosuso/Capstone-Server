package com.knu.sosuso.capstone.domain.scrap.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.sosuso.capstone.domain.scrap.entity.Scrap;
import com.knu.sosuso.capstone.domain.auth.User;
import com.knu.sosuso.capstone.domain.video.entity.Video;
import com.knu.sosuso.capstone.domain.scrap.dto.request.CreateScrapRequest;
import com.knu.sosuso.capstone.domain.scrap.dto.response.CreateScrapResponse;
import com.knu.sosuso.capstone.domain.video.dto.response.VideoSummaryResponse;
import com.knu.sosuso.capstone.global.exception.BusinessException;
import com.knu.sosuso.capstone.global.exception.error.AuthenticationError;
import com.knu.sosuso.capstone.global.exception.error.ScrapError;
import com.knu.sosuso.capstone.global.exception.error.VideoError;
import com.knu.sosuso.capstone.domain.scrap.repository.ScrapRepository;
import com.knu.sosuso.capstone.domain.auth.UserRepository;
import com.knu.sosuso.capstone.domain.video.repository.VideoRepository;
import com.knu.sosuso.capstone.global.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Service
public class ScrapService {

    private final ScrapRepository scrapRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

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

        return new CreateScrapResponse(scrap.getId());
    }

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

        scrapRepository.deleteById(scrapId);
    }

    /**
     * 스크랩한 영상 목록 조회 (삭제된 영상 처리 포함)
     */
    @Transactional(readOnly = true)
    public List<VideoSummaryResponse> getScrappedVideos(String token) {
        try {
            // 1. 토큰 검증
            if (!jwtUtil.isValidToken(token)) {
                throw new BusinessException(AuthenticationError.INVALID_TOKEN);
            }

            Long userId = jwtUtil.getUserId(token);

            // 2. 사용자의 스크랩 목록 조회
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

                    // 삭제된 영상 처리
                    if (video.isDeleted()) {
                        log.info("삭제된 영상 발견: videoId={}, apiVideoId={}",
                                video.getId(), video.getApiVideoId());
                        results.add(createDeletedVideoResponse(video));
                        continue;
                    }

                    // 정상 영상 처리
                    VideoSummaryResponse summaryResponse = convertVideoToSummaryResponse(video);
                    results.add(summaryResponse);

                    log.debug("스크랩 영상 변환 완료: apiVideoId={}, title={}",
                            video.getApiVideoId(), video.getTitle());

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
            throw new RuntimeException("스크랩 영상 조회 중 오류 발생", e);
        }
    }

    /**
     * 삭제된 영상용 응답 생성
     */
    private VideoSummaryResponse createDeletedVideoResponse(Video video) {
        VideoSummaryResponse.Video videoDto = new VideoSummaryResponse.Video(
                video.getApiVideoId(),
                "[삭제된 영상] " + video.getTitle(),  // 제목 앞에 표시
                "이 영상은 삭제되었거나 비공개 처리되었습니다.",
                video.getUploadedAt(),
                video.getThumbnailUrl(),  // 썸네일은 유지 (캐시된 것)
                0L, 0L, 0  // 조회수, 좋아요, 댓글 수는 0으로
        );

        VideoSummaryResponse.Channel channelDto = new VideoSummaryResponse.Channel(
                video.getChannelId(),
                video.getChannelName(),
                video.getChannelThumbnailUrl(),
                0L  // 구독자 수도 0으로
        );

        VideoSummaryResponse.Analysis analysisDto = new VideoSummaryResponse.Analysis(
                "이 영상은 삭제되었습니다.",
                null,  // 감정 분포 없음
                List.of()  // 키워드 없음
        );

        return new VideoSummaryResponse(videoDto, channelDto, analysisDto);
    }

    /**
     * Video 엔티티를 VideoSummaryResponse로 변환
     */
    private VideoSummaryResponse convertVideoToSummaryResponse(Video video) {
        try {
            // SentimentDistribution 변환
            VideoSummaryResponse.SentimentDistribution sentimentDistribution = null;
            if (video.getSentimentDistribution() != null && !video.getSentimentDistribution().trim().isEmpty()) {
                try {
                    Map<String, Double> sentimentMap = objectMapper.readValue(
                            video.getSentimentDistribution(),
                            new TypeReference<>() {}
                    );

                    sentimentDistribution = new VideoSummaryResponse.SentimentDistribution(
                            sentimentMap.getOrDefault("positive", 0.0),
                            sentimentMap.getOrDefault("negative", 0.0),
                            sentimentMap.getOrDefault("other", 0.0)
                    );
                } catch (Exception e) {
                    log.warn("SentimentDistribution 파싱 실패: videoId={}, error={}",
                            video.getId(), e.getMessage());
                }
            }

            // Keywords 변환
            List<String> keywords = new ArrayList<>();
            if (video.getKeywords() != null && !video.getKeywords().trim().isEmpty()) {
                try {
                    keywords = objectMapper.readValue(
                            video.getKeywords(),
                            new TypeReference<>() {}
                    );
                } catch (Exception e) {
                    log.warn("Keywords 파싱 실패: videoId={}, error={}", video.getId(), e.getMessage());
                }
            }

            var videoDto = new VideoSummaryResponse.Video(
                    video.getApiVideoId(),
                    video.getTitle(),
                    video.getDescription(),
                    video.getUploadedAt(),
                    video.getThumbnailUrl(),
                    parseLong(video.getViewCount()),
                    parseLong(video.getLikeCount()),
                    parseInt(video.getCommentCount())
            );

            var channelDto = new VideoSummaryResponse.Channel(
                    video.getChannelId(),
                    video.getChannelName(),
                    video.getChannelThumbnailUrl(),
                    parseLong(video.getSubscriberCount())
            );

            var analysisDto = new VideoSummaryResponse.Analysis(
                    video.getSummation(),
                    sentimentDistribution,
                    keywords
            );

            return new VideoSummaryResponse(videoDto, channelDto, analysisDto);

        } catch (Exception e) {
            log.error("VideoSummaryResponse 변환 실패: videoId={}, error={}",
                    video.getId(), e.getMessage(), e);
            throw new RuntimeException("영상 응답 변환 중 오류 발생", e);
        }
    }

    /**
     * 문자열을 Long으로 안전하게 변환
     */
    private Long parseLong(String value) {
        try {
            return value != null && !value.trim().isEmpty() ? Long.parseLong(value) : 0L;
        } catch (NumberFormatException e) {
            log.warn("Long 변환 실패: value={}", value);
            return 0L;
        }
    }

    /**
     * 문자열을 Integer로 안전하게 변환
     */
    private Integer parseInt(String value) {
        try {
            return value != null && !value.trim().isEmpty() ? Integer.parseInt(value) : 0;
        } catch (NumberFormatException e) {
            log.warn("Integer 변환 실패: value={}", value);
            return 0;
        }
    }
}