package com.knu.sosuso.capstone.service;

import com.knu.sosuso.capstone.domain.FavoriteChannel;
import com.knu.sosuso.capstone.domain.User;
import com.knu.sosuso.capstone.dto.request.RegisterFavoriteChannelRequest;
import com.knu.sosuso.capstone.dto.response.VideoSummaryResponse;
import com.knu.sosuso.capstone.dto.response.detail.DetailPageResponse;
import com.knu.sosuso.capstone.dto.response.favorite_channel.CancelFavoriteChannelResponse;
import com.knu.sosuso.capstone.dto.response.favorite_channel.FavoriteChannelListResponse;
import com.knu.sosuso.capstone.dto.response.favorite_channel.RegisterFavoriteChannelResponse;
import com.knu.sosuso.capstone.exception.BusinessException;
import com.knu.sosuso.capstone.exception.error.AuthenticationError;
import com.knu.sosuso.capstone.exception.error.FavoriteChannelError;
import com.knu.sosuso.capstone.repository.FavoriteChannelRepository;
import com.knu.sosuso.capstone.repository.UserRepository;
import com.knu.sosuso.capstone.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final TrendingService trendingService;

    @Transactional
    public RegisterFavoriteChannelResponse registerFavoriteChannel(String token, RegisterFavoriteChannelRequest registerFavoriteChannelRequest) {
        if (!jwtUtil.isValidToken(token)) {
            throw new BusinessException(AuthenticationError.INVALID_TOKEN);
        }

        Long userId = jwtUtil.getUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(AuthenticationError.USER_NOT_FOUND));

        boolean existsFavoriteChannel = favoriteChannelRepository.existsByUserIdAndApiChannelId(userId, registerFavoriteChannelRequest.apiChannelId());
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
            String apiChannelName = favoriteChannel.getApiChannelName();
            String apiChannelThumbnail = favoriteChannel.getApiChannelThumbnail();
            FavoriteChannelListResponse favoriteChannelListResponse
                    = new FavoriteChannelListResponse(favoriteChannelId, apiChannelName, apiChannelThumbnail);
            favoriteChannelListResponses.add(favoriteChannelListResponse);
        }

        return favoriteChannelListResponses;
    }

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

    @Transactional
    public VideoSummaryResponse processLatestVideoFromFavoriteChannel(String token, String apiChannelId){

        String latestApiVideoId = channelService.getlatestApiVideoId(apiChannelId);

        DetailPageResponse response = videoProcessingService.processVideoToSearchResult(token, latestApiVideoId, true);

        return trendingService.convertToVideoSummaryResponse(response);
    }

}
