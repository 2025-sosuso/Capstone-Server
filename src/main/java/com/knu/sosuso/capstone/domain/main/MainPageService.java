package com.knu.sosuso.capstone.domain.main;

import com.knu.sosuso.capstone.domain.channel.entity.FavoriteChannel;
import com.knu.sosuso.capstone.domain.channel.service.FavoriteChannelService;
import com.knu.sosuso.capstone.domain.scrap.service.ScrapService;
import com.knu.sosuso.capstone.domain.video.dto.response.VideoSummaryResponse;
import com.knu.sosuso.capstone.domain.channel.dto.response.FavoriteChannelListResponse;
import com.knu.sosuso.capstone.domain.channel.dto.response.FavoriteVideoInfoResponse;
import com.knu.sosuso.capstone.global.exception.BusinessException;
import com.knu.sosuso.capstone.domain.channel.repository.FavoriteChannelRepository;
import com.knu.sosuso.capstone.global.security.jwt.JwtUtil;
import com.knu.sosuso.capstone.domain.video.service.PopularVideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class MainPageService {

    private final FavoriteChannelService favoriteChannelService;
    private final PopularVideoService popularVideoService;
    private final ScrapService scrapService;
    private final FavoriteChannelRepository favoriteChannelRepository;
    private final JwtUtil jwtUtil;

    /**
     * 관심 채널 섹션 데이터 조회
     * - 토큰이 없거나 유효하지 않으면 빈 데이터 반환
     */
    @Transactional
    public FavoriteChannelSectionResponse getFavoriteChannelResponse(String token) {
        log.info("관심 채널 섹션 데이터 조회 시작");

        try {
            // 토큰 검증
            if (!StringUtils.hasText(token) || !jwtUtil.isValidToken(token)) {
                log.info("토큰 없음 또는 유효하지 않음 - 빈 관심 채널 반환");
                return createEmptyFavoriteChannelResponse();
            }

            Long userId = jwtUtil.getUserId(token);

            // 관심 채널 목록 조회
            List<FavoriteChannelListResponse> favoriteChannelList =
                    favoriteChannelService.getFavoriteChannelList(token);

            if (favoriteChannelList.isEmpty()) {
                log.info("관심 채널이 없습니다");
                return new FavoriteChannelSectionResponse(
                        new ArrayList<>(),
                        null
                );
            }

            // 첫 번째 채널의 최신 영상 조회
            FavoriteChannelListResponse selectedChannel = favoriteChannelList.get(0);
            log.debug("선택된 채널: {}", selectedChannel.apiChannelName());

            long favoriteChannelId = selectedChannel.favoriteChannelId();
            Optional<FavoriteChannel> favoriteChannelOpt =
                    favoriteChannelRepository.findByIdAndUserId(favoriteChannelId, userId);

            if (favoriteChannelOpt.isPresent()) {
                String apiChannelId = favoriteChannelOpt.get().getApiChannelId();

                // 해당 채널의 최신 비디오 1개 조회
                FavoriteVideoInfoResponse channelVideo =
                        favoriteChannelService.processLatestVideoFromFavoriteChannel(token, apiChannelId);

                log.info("관심 채널 섹션 조회 성공: 채널 수={}, 최신 영상 있음",
                        favoriteChannelList.size());

                return new FavoriteChannelSectionResponse(
                        favoriteChannelList,
                        channelVideo
                );
            }

            log.warn("관심 채널을 찾을 수 없음: favoriteChannelId={}", favoriteChannelId);
            return new FavoriteChannelSectionResponse(
                    favoriteChannelList,
                    null
            );

        } catch (BusinessException e) {
            log.error("관심 채널 섹션 조회 비즈니스 예외: {}", e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("관심 채널 섹션 조회 실패: {}", e.getMessage(), e);
            return createEmptyFavoriteChannelResponse();
        }
    }

    /**
     * 인기 영상 섹션 데이터 조회 (최대 3개)
     * - 자체 알고리즘 기반 (검색/조회 + 스크랩 데이터)
     * - 인증 불필요, 모든 사용자에게 동일하게 제공
     */
    @Transactional
    public List<VideoSummaryResponse> getTrendingVideos(String token) {
        log.info("인기 영상 섹션 데이터 조회 시작");

        try {
            List<VideoSummaryResponse> popularVideos =
                    popularVideoService.getPopularVideos(token, 3);

            log.info("인기 영상 섹션 조회 완료: 영상 수={}", popularVideos.size());
            return popularVideos;

        } catch (BusinessException e) {
            log.error("인기 영상 섹션 조회 비즈니스 예외: {}", e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("인기 영상 섹션 조회 실패: {}", e.getMessage(), e);
            return new ArrayList<>(); // 실패해도 빈 리스트 반환하여 다른 섹션에 영향 없도록
        }
    }

    /**
     * 스크랩 섹션 데이터 조회 (최대 3개)
     * - 인증 필수
     */
    @Transactional
    public List<VideoSummaryResponse> getScrapVideos(String token) {
        log.info("스크랩 섹션 데이터 조회 시작");

        try {
            List<VideoSummaryResponse> allScrapVideos = scrapService.getScrappedVideos(token);

            // 최대 3개까지만 반환
            int maxSize = Math.min(allScrapVideos.size(), 3);
            List<VideoSummaryResponse> limitedScrapVideos = allScrapVideos.subList(0, maxSize);

            log.info("스크랩 섹션 조회 완료: 전체={}, 반환={}",
                    allScrapVideos.size(), limitedScrapVideos.size());

            return limitedScrapVideos;

        } catch (BusinessException e) {
            log.error("스크랩 섹션 조회 비즈니스 예외: {}", e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("스크랩 섹션 조회 실패: {}", e.getMessage(), e);
            return new ArrayList<>(); // 실패해도 빈 리스트 반환
        }
    }

    /**
     * 빈 관심 채널 응답 생성
     */
    private FavoriteChannelSectionResponse createEmptyFavoriteChannelResponse() {
        return new FavoriteChannelSectionResponse(
                Collections.emptyList(),
                null
        );
    }
}