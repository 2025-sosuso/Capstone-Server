package com.knu.sosuso.capstone.domain.channel.service;

import com.knu.sosuso.capstone.domain.channel.entity.FavoriteChannel;
import com.knu.sosuso.capstone.domain.auth.User;
import com.knu.sosuso.capstone.domain.channel.dto.request.RegisterFavoriteChannelRequest;
import com.knu.sosuso.capstone.domain.channel.dto.response.CancelFavoriteChannelResponse;
import com.knu.sosuso.capstone.domain.channel.dto.response.FavoriteChannelListResponse;
import com.knu.sosuso.capstone.domain.channel.dto.response.FavoriteVideoInfoResponse;
import com.knu.sosuso.capstone.domain.channel.dto.response.RegisterFavoriteChannelResponse;
import com.knu.sosuso.capstone.domain.video.entity.Video;
import com.knu.sosuso.capstone.domain.video.repository.VideoRepository;
import com.knu.sosuso.capstone.global.exception.BusinessException;
import com.knu.sosuso.capstone.global.exception.error.AuthenticationError;
import com.knu.sosuso.capstone.global.exception.error.FavoriteChannelError;
import com.knu.sosuso.capstone.domain.channel.repository.FavoriteChannelRepository;
import com.knu.sosuso.capstone.domain.auth.UserRepository;
import com.knu.sosuso.capstone.global.exception.error.VideoError;
import com.knu.sosuso.capstone.global.security.jwt.JwtUtil;
import com.knu.sosuso.capstone.domain.video.service.VideoProcessingService;
import com.knu.sosuso.capstone.global.service.mapper.ResponseMappingService;
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
    private final VideoRepository videoRepository;
    private final ResponseMappingService responseMappingService;

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
        // 1. 최신 영상 ID 가져오기
        String latestApiVideoId = channelService.getlatestApiVideoId(apiChannelId);

        // 2. 영상 처리 (DB에 저장/업데이트) - 반환값은 사용하지 않음
        videoProcessingService.processVideoToSearchResult(token, latestApiVideoId, true);

        Video video = videoRepository.findByApiVideoId(latestApiVideoId)
                .orElseThrow(() -> new BusinessException(VideoError.VIDEO_NOT_FOUND));

        return responseMappingService.mapDbToFavoriteVideoInfoResponse(token, video);
    }
}