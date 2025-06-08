package com.knu.sosuso.capstone.service;

import com.knu.sosuso.capstone.domain.FavoriteChannel;
import com.knu.sosuso.capstone.domain.User;
import com.knu.sosuso.capstone.dto.request.RegisterFavoriteChannelRequest;
import com.knu.sosuso.capstone.dto.response.favorite_channel.CancelFavoriteChannelResponse;
import com.knu.sosuso.capstone.dto.response.favorite_channel.RegisterFavoriteChannelResponse;
import com.knu.sosuso.capstone.repository.FavoriteChannelRepository;
import com.knu.sosuso.capstone.repository.UserRepository;
import com.knu.sosuso.capstone.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class FavoriteChannelService {

    private final FavoriteChannelRepository favoriteChannelRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    // Todo custom exception으로 바꾸기
    @Transactional
    public RegisterFavoriteChannelResponse registerFavoriteChannel(String token, RegisterFavoriteChannelRequest registerFavoriteChannelRequest) {
        if (!jwtUtil.isValidToken(token)) {
            throw new RuntimeException("유효하지 않은 토큰입니다.");
        }

        Long userId = jwtUtil.getUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다."));

        boolean existsFavoriteChannel = favoriteChannelRepository.existsByUserIdAndApiChannelId(userId, registerFavoriteChannelRequest.apiChannelId());
        if (existsFavoriteChannel) {
            throw new RuntimeException("이미 관심 채널로 등록되었습니다.");
        }

        String apiChannelId = registerFavoriteChannelRequest.apiChannelId();
        String apiChannelName = registerFavoriteChannelRequest.apiChannelName();
        FavoriteChannel favoriteChannel = FavoriteChannel.builder()
                .user(user)
                .apiChannelId(apiChannelId)
                .apiChannelName(apiChannelName)
                .build();

        FavoriteChannel savedFavoriteChannel = favoriteChannelRepository.save(favoriteChannel);
        Long favoriteChannelId = savedFavoriteChannel.getId();
        return new RegisterFavoriteChannelResponse(favoriteChannelId, apiChannelId);
    }

    // Todo custom exception으로 바꾸기
    @Transactional
    public CancelFavoriteChannelResponse cancelFavoriteChannel(String token, Long favoriteChannelId) {
        if (!jwtUtil.isValidToken(token)) {
            throw new RuntimeException("유효하지 않은 토큰입니다.");
        }

        Long userId = jwtUtil.getUserId(token);

        FavoriteChannel favoriteChannel = favoriteChannelRepository.findById(favoriteChannelId)
                .orElseThrow(() -> new RuntimeException("관심 채널로 등록되어 있지 않은 채널입니다."));

        if (!favoriteChannel.getUser().getId().equals(userId)) {
            throw new RuntimeException("본인의 관심 채널만 취소할 수 있습니다.");
        }

        favoriteChannelRepository.deleteById(favoriteChannelId);

        return new CancelFavoriteChannelResponse(favoriteChannelId);
    }
}
