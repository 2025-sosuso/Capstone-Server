package com.knu.sosuso.capstone.service;

import com.knu.sosuso.capstone.domain.FavoriteChannel;
import com.knu.sosuso.capstone.domain.Scrap;
import com.knu.sosuso.capstone.repository.FavoriteChannelRepository;
import com.knu.sosuso.capstone.repository.ScrapRepository;
import com.knu.sosuso.capstone.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserDataService {

    private final ScrapRepository scrapRepository;
    private final FavoriteChannelRepository favoriteChannelRepository;
    private final JwtUtil jwtUtil;

    /**
     * 사용자의 스크랩 ID 조회
     * @param token 사용자 토큰 (nullable)
     * @param apiVideoId 비디오 ID
     * @return 스크랩 ID 또는 null
     */
    public Long getUserScrapId(String token, String apiVideoId) {
        if (token == null || !jwtUtil.isValidToken(token)) {
            return null;
        }

        try {
            Long userId = jwtUtil.getUserId(token);
            Optional<Scrap> scrap = scrapRepository.findByUserIdAndApiVideoId(userId, apiVideoId);
            return scrap.map(Scrap::getId).orElse(null);
        } catch (Exception e) {
            log.warn("스크랩 ID 조회 실패: apiVideoId={}, error={}", apiVideoId, e.getMessage());
            return null;
        }
    }

    /**
     * 사용자의 관심 채널 ID 조회
     * @param token 사용자 토큰 (nullable)
     * @param apiChannelId 채널 ID
     * @return 관심 채널 ID 또는 null
     */
    public Long getUserFavoriteChannelId(String token, String apiChannelId) {
        if (token == null || !jwtUtil.isValidToken(token)) {
            return null;
        }

        try {
            Long userId = jwtUtil.getUserId(token);
            Optional<FavoriteChannel> favoriteChannel = favoriteChannelRepository.findByUserIdAndApiChannelId(userId, apiChannelId);
            return favoriteChannel.map(FavoriteChannel::getId).orElse(null);
        } catch (Exception e) {
            log.warn("관심 채널 ID 조회 실패: apiChannelId={}, error={}", apiChannelId, e.getMessage());
            return null;
        }
    }
}
