package com.knu.sosuso.capstone.service;

import com.knu.sosuso.capstone.domain.Comment;
import com.knu.sosuso.capstone.domain.FavoriteChannel;
import com.knu.sosuso.capstone.dto.response.MainPageResponse;
import com.knu.sosuso.capstone.dto.response.VideoSummaryResponse;
import com.knu.sosuso.capstone.dto.response.detail.DetailCommentDto;
import com.knu.sosuso.capstone.dto.response.favorite_channel.FavoriteChannelListResponse;
import com.knu.sosuso.capstone.dto.response.favorite_channel.FavoriteVideoInfoResponse;
import com.knu.sosuso.capstone.exception.BusinessException;
import com.knu.sosuso.capstone.exception.error.AuthenticationError;
import com.knu.sosuso.capstone.repository.FavoriteChannelRepository;
import com.knu.sosuso.capstone.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class MainPageService {

    private final FavoriteChannelService favoriteChannelService;
    private final TrendingService trendingService;
    private final ScrapService scrapService;
    private final ResponseMappingService responseMappingService;
    private final FavoriteChannelRepository favoriteChannelRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public MainPageResponse getMainPageData(String token) {
        log.info("메인 페이지 데이터 조회 시작");

        try {
            if (StringUtils.hasText(token) && jwtUtil.isValidToken(token)) {
                // 인증된 사용자용 메인 페이지
                return getAuthenticatedMainPageData(token);
            } else {
                // 비인증 사용자용 메인 페이지
                return getGuestMainPageData();
            }

        } catch (Exception e) {
            log.error("메인 페이지 데이터 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("메인 페이지 데이터 조회 중 오류 발생", e);
        }
    }

    // 인증된 사용자용 메인 페이지 데이터
    @Transactional
    public MainPageResponse getAuthenticatedMainPageData(String token) {
        log.info("인증된 사용자 메인 페이지 데이터 조회");

        List<VideoSummaryResponse> scrapVideos = getScrapVideos(token);
        List<VideoSummaryResponse> trendingVideos = getTrendingVideos();
        MainPageResponse.FavoriteChannelResponse favoriteChannelResponse = getFavoriteChannelResponse(token);

        MainPageResponse response = new MainPageResponse(
                favoriteChannelResponse,
                trendingVideos,
                scrapVideos
        );

        log.info("인증된 사용자 메인 페이지 데이터 조회 완료: 스크랩={}, 트렌딩={}, 즐겨찾기 채널={}",
                scrapVideos.size(),
                trendingVideos.size(),
                favoriteChannelResponse.favoriteChannelList() != null
                        ? favoriteChannelResponse.favoriteChannelList().size() : 0);

        return response;
    }

    // 비인증 사용자용 메인 페이지 데이터
    @Transactional
    public MainPageResponse getGuestMainPageData() {
        log.info("비인증 사용자 메인 페이지 데이터 조회");

        List<VideoSummaryResponse> trendingVideos = getTrendingVideos();
        MainPageResponse.FavoriteChannelResponse emptyFavoriteChannelResponse = createEmptyFavoriteChannelResponse();

        MainPageResponse response = new MainPageResponse(
                emptyFavoriteChannelResponse,
                trendingVideos,
                Collections.emptyList() // 스크랩 비디오 없음
        );

        log.info("비인증 사용자 메인 페이지 데이터 조회 완료: 트렌딩={}", trendingVideos.size());

        return response;
    }

    // 트렌딩 비디오는 토큰 없이도 조회 가능하도록 오버로드
    @Transactional
    public List<VideoSummaryResponse> getTrendingVideos() {
        return getTrendingVideos(null);
    }

    // 빈 즐겨찾기 채널 응답 생성
    private MainPageResponse.FavoriteChannelResponse createEmptyFavoriteChannelResponse() {
        return new MainPageResponse.FavoriteChannelResponse(
                Collections.emptyList(),
                null,
                null
        );
    }


    /**
     * 스크랩된 비디오 목록 조회 (최대 3개)
     */
    @Transactional
    public List<VideoSummaryResponse> getScrapVideos(String token) {
        try {
            List<VideoSummaryResponse> allScrapVideos = scrapService.getScrappedVideos(token);

            // 최대 3개까지만 반환
            int maxSize = Math.min(allScrapVideos.size(), 3);
            List<VideoSummaryResponse> limitedScrapVideos = allScrapVideos.subList(0, maxSize);

            log.debug("스크랩 비디오 조회 완료: 전체={}, 반환={}", allScrapVideos.size(), limitedScrapVideos.size());
            return limitedScrapVideos;

        } catch (Exception e) {
            log.error("스크랩 비디오 조회 실패: {}", e.getMessage(), e);
            return new ArrayList<>(); // 실패해도 빈 리스트 반환하여 다른 섹션에 영향주지 않음
        }
    }

    /**
     * 트렌딩 비디오 목록 조회 (최대 3개)
     */
    @Transactional
    public List<VideoSummaryResponse> getTrendingVideos(String token) {
        try {
            List<VideoSummaryResponse> trendingVideos = trendingService.getTrendingVideoWithComments(token, "latest", 3);
            log.debug("트렌딩 비디오 조회 완료: 개수={}", trendingVideos.size());
            return trendingVideos;

        } catch (Exception e) {
            log.error("트렌딩 비디오 조회 실패: {}", e.getMessage(), e);
            return new ArrayList<>(); // 실패해도 빈 리스트 반환
        }
    }

    /**
     * 즐겨찾기 채널 응답 생성
     */
    @Transactional
    public MainPageResponse.FavoriteChannelResponse getFavoriteChannelResponse(String token) {
        if (!jwtUtil.isValidToken(token)) {
            throw new BusinessException(AuthenticationError.INVALID_TOKEN);
        }

        Long userId = jwtUtil.getUserId(token);

        // 즐겨찾기 채널 목록 조회
        List<FavoriteChannelListResponse> favoriteChannelList = favoriteChannelService.getFavoriteChannelList(token);

        if (favoriteChannelList.isEmpty()) {
            log.info("즐겨찾기 채널이 없습니다");
            return new MainPageResponse.FavoriteChannelResponse(
                    new ArrayList<>(),
                    null,
                    new ArrayList<>()
            );
        }

        FavoriteChannelListResponse selectedChannel = favoriteChannelList.get(0);
        log.debug("선택된 채널: {}", selectedChannel.apiChannelName());

        long favoriteChannelId = selectedChannel.favoriteChannelId();

        Optional<FavoriteChannel> favoriteChannelOpt = favoriteChannelRepository.findByIdAndUserId(favoriteChannelId, userId);

        try {
            if (favoriteChannelOpt.isPresent()) {
                String apiChannelId = favoriteChannelOpt.get().getApiChannelId();

                // 해당 채널의 최신 비디오 하나와 댓글 조회
                FavoriteVideoInfoResponse channelVideo;
                List<DetailCommentDto> topComments = new ArrayList<>();

                // 채널의 최신 비디오를 가져오는 로직
                channelVideo = favoriteChannelService.processLatestVideoFromFavoriteChannel(token, apiChannelId);

                if (channelVideo != null) {
                    topComments = getTopCommentsForVideo(channelVideo.video().id());
                }

                return new MainPageResponse.FavoriteChannelResponse(
                        favoriteChannelList,
                        channelVideo,
                        topComments
                );
            }
        } catch (Exception e) {
            log.error("즐겨찾기 채널 응답 생성 실패: {}", e.getMessage(), e);
            return new MainPageResponse.FavoriteChannelResponse(
                    new ArrayList<>(),
                    null,
                    new ArrayList<>()
            );
        }
        return null;
    }

    /**
     * 비디오의 상위 댓글 조회 (최대 5개)
     */
    @Transactional
    public List<DetailCommentDto> getTopCommentsForVideo(String videoId) {
        try {
            log.debug("비디오 상위 댓글 조회 시작: videoId={}", videoId);

            // DB에서 해당 비디오의 댓글 조회 (좋아요 순으로 정렬)
            List<Comment> comments = responseMappingService.mapToTopCommentsFromDb(videoId);

            if (comments.isEmpty()) {
                log.debug("해당 비디오에 댓글이 없습니다: videoId={}", videoId);
                return new ArrayList<>();
            }

            // Comment 엔티티를 DetailCommentDto로 변환
            List<DetailCommentDto> topComments = comments.stream()
                    .map(comment -> new DetailCommentDto(
                            comment.getApiCommentId(),
                            comment.getWriter(),
                            comment.getCommentContent(),
                            comment.getLikeCount(),
                            comment.getSentimentType() != null ? comment.getSentimentType().name().toLowerCase() : null,
                            comment.getWrittenAt()
                    ))
                    .collect(Collectors.toList());

            log.debug("상위 댓글 조회 완료: videoId={}, 댓글 수={}", videoId, topComments.size());
            return topComments;

        } catch (Exception e) {
            log.error("상위 댓글 조회 실패: videoId={}, error={}", videoId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}