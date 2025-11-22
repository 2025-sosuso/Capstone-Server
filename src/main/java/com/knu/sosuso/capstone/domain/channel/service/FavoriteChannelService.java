package com.knu.sosuso.capstone.domain.channel.service;

import com.knu.sosuso.capstone.domain.channel.entity.FavoriteChannel;
import com.knu.sosuso.capstone.domain.auth.User;
import com.knu.sosuso.capstone.domain.channel.dto.request.RegisterFavoriteChannelRequest;
import com.knu.sosuso.capstone.domain.comment.dto.CommentDto;
import com.knu.sosuso.capstone.domain.common.dto.SentimentDistribution;
import com.knu.sosuso.capstone.domain.detail.dto.DetailPageResponse;
import com.knu.sosuso.capstone.domain.channel.dto.response.CancelFavoriteChannelResponse;
import com.knu.sosuso.capstone.domain.channel.dto.response.FavoriteChannelListResponse;
import com.knu.sosuso.capstone.domain.channel.dto.response.FavoriteVideoInfoResponse;
import com.knu.sosuso.capstone.domain.channel.dto.response.RegisterFavoriteChannelResponse;
import com.knu.sosuso.capstone.global.exception.BusinessException;
import com.knu.sosuso.capstone.global.exception.error.AuthenticationError;
import com.knu.sosuso.capstone.global.exception.error.CommonError;
import com.knu.sosuso.capstone.global.exception.error.FavoriteChannelError;
import com.knu.sosuso.capstone.domain.channel.repository.FavoriteChannelRepository;
import com.knu.sosuso.capstone.domain.auth.UserRepository;
import com.knu.sosuso.capstone.global.security.jwt.JwtUtil;
import com.knu.sosuso.capstone.domain.video.service.VideoProcessingService;
import com.knu.sosuso.capstone.global.service.mapper.VideoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class FavoriteChannelService {

    private final FavoriteChannelRepository favoriteChannelRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final ChannelService channelService;
    private final VideoProcessingService videoProcessingService;
    private final VideoMapper videoMapper;

    /**
     * 관심 채널 등록
     * 등록 후 관심 채널 목록 캐시를 무효화
     */
    @CacheEvict(value = "favoriteChannels",
            key = "'user-' + #token.hashCode()")
    @Transactional
    public RegisterFavoriteChannelResponse registerFavoriteChannel(String token, RegisterFavoriteChannelRequest registerFavoriteChannelRequest) {
        if (!jwtUtil.isValidToken(token)) {
            throw new BusinessException(AuthenticationError.INVALID_TOKEN);
        }

        Long userId = jwtUtil.getUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(AuthenticationError.USER_NOT_FOUND));

        boolean existsFavoriteChannel = favoriteChannelRepository.existsByUserIdAndApiChannelId(
                userId, registerFavoriteChannelRequest.apiChannelId());
        if (existsFavoriteChannel) {
            throw new BusinessException(FavoriteChannelError.FAVORITE_CHANNEL_ALREADY_EXISTS);
        }

        String apiChannelId = registerFavoriteChannelRequest.apiChannelId();
        String apiChannelName = registerFavoriteChannelRequest.apiChannelName();
        String apiChannelThumbnail = registerFavoriteChannelRequest.apiChannelThumbnail();
        FavoriteChannel favoriteChannel = FavoriteChannel.builder()
                .user(user)
                .apiChannelId(apiChannelId)
                .apiChannelName(apiChannelName)
                .apiChannelThumbnail(apiChannelThumbnail)
                .build();

        FavoriteChannel savedFavoriteChannel = favoriteChannelRepository.save(favoriteChannel);
        Long favoriteChannelId = savedFavoriteChannel.getId();
        return new RegisterFavoriteChannelResponse(favoriteChannelId, apiChannelId);
    }

    /**
     * 사용자의 관심 채널 목록 조회
     * 자주 조회되지만 변경이 적으므로 캐싱 적용 (2시간 유지)
     * 사용자별로 캐싱
     */
    @Cacheable(value = "favoriteChannels",
            key = "'user-' + #token.hashCode()",
            condition = "#token != null")
    @Transactional
    public List<FavoriteChannelListResponse> getFavoriteChannelList(String token) {
        if (!jwtUtil.isValidToken(token)) {
            throw new BusinessException(AuthenticationError.INVALID_TOKEN);
        }

        Long userId = jwtUtil.getUserId(token);
        List<FavoriteChannel> favoriteChannelList = favoriteChannelRepository.findByUserId(userId);

        List<FavoriteChannelListResponse> favoriteChannelListResponses = new ArrayList<>();
        for (FavoriteChannel favoriteChannel : favoriteChannelList) {
            Long favoriteChannelId = favoriteChannel.getId();
            String apiChannelId = favoriteChannel.getApiChannelId();
            String apiChannelName = favoriteChannel.getApiChannelName();
            String apiChannelThumbnail = favoriteChannel.getApiChannelThumbnail();
            FavoriteChannelListResponse favoriteChannelListResponse
                    = new FavoriteChannelListResponse(favoriteChannelId, apiChannelId, apiChannelName, apiChannelThumbnail);
            favoriteChannelListResponses.add(favoriteChannelListResponse);
        }

        return favoriteChannelListResponses;
    }
    /**
     * 관심 채널 취소
     * 취소 후 관심 채널 목록 캐시를 무효화
     */
    @CacheEvict(value = "favoriteChannels",
            key = "'user-' + #token.hashCode()")
    @Transactional
    public CancelFavoriteChannelResponse cancelFavoriteChannel(String token, Long favoriteChannelId) {
        if (!jwtUtil.isValidToken(token)) {
            throw new BusinessException(AuthenticationError.INVALID_TOKEN);
        }

        Long userId = jwtUtil.getUserId(token);

        FavoriteChannel favoriteChannel = favoriteChannelRepository.findById(favoriteChannelId)
                .orElseThrow(() -> new BusinessException(FavoriteChannelError.FAVORITE_CHANNEL_NOT_FOUND));

        if (!favoriteChannel.getUser().getId().equals(userId)) {
            throw new BusinessException(FavoriteChannelError.FORBIDDEN_FAVORITE_CHANNEL_DELETE);
        }

        favoriteChannelRepository.deleteById(favoriteChannelId);

        return new CancelFavoriteChannelResponse(favoriteChannelId);
    }

    /**
     * 관심 채널의 최신 비디오 정보 처리
     * 무거운 작업이므로 캐싱 적용 (30분 유지)
     * 토큰과 채널ID 조합으로 캐싱
     */
    @Cacheable(value = "videoDetail",
            key = "'favorite-video-' + #apiChannelId + '-' + (#token != null ? #token.hashCode() : 'anonymous')",
            unless = "#result == null")
    @Transactional
    public FavoriteVideoInfoResponse processLatestVideoFromFavoriteChannel(String token, String apiChannelId) {
        String latestApiVideoId = channelService.getlatestApiVideoId(apiChannelId);
        DetailPageResponse response = videoProcessingService.processVideoToSearchResult(token, latestApiVideoId, true);
        return convertToVideoSummaryFavoriteResponse(response);
    }

    public FavoriteVideoInfoResponse convertToVideoSummaryFavoriteResponse(DetailPageResponse detailResponse) {
        try {
            var video = detailResponse.video();
            var channel = detailResponse.channel();
            var analysis = detailResponse.analysis();

            FavoriteVideoInfoResponse.Video videoDto = new FavoriteVideoInfoResponse.Video(
                    video.id(),
                    video.title(),
                    video.description(),
                    video.publishedAt(),
                    video.thumbnailUrl(),
                    video.viewCount(),
                    video.likeCount(),
                    video.commentCount()
            );

            FavoriteVideoInfoResponse.Channel channelDto = new FavoriteVideoInfoResponse.Channel(
                    channel.id(),
                    channel.title(),
                    channel.thumbnailUrl(),
                    channel.subscriberCount()
            );

            SentimentDistribution sentimentDto = null;
            if (analysis != null && analysis.sentimentDistribution() != null) {
                var s = analysis.sentimentDistribution();
                sentimentDto = new SentimentDistribution(
                        s.positive(), s.negative(), s.other());
            }

            List<String> keywords = (analysis != null && analysis.keywords() != null)
                    ? analysis.keywords() : List.of();
            String summary = (analysis != null) ? analysis.summary() : null;

            List<CommentDto> topComments = List.of();
            if (analysis != null && analysis.topComments() != null) {
                topComments = analysis.topComments();
            }

            FavoriteVideoInfoResponse.Analysis analysisDto = new FavoriteVideoInfoResponse.Analysis(
                    summary,
                    sentimentDto,
                    keywords,
                    topComments
            );

            return new FavoriteVideoInfoResponse(videoDto, channelDto, analysisDto);

        } catch (Exception e) {
            log.error("VideoSummaryResponse 변환 실패: error={}", e.getMessage(), e);
            throw new BusinessException(CommonError.DATA_CONVERSION_ERROR);
        }
    }
}