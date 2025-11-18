package com.knu.sosuso.capstone.domain.video.service;

import com.knu.sosuso.capstone.domain.channel.entity.FavoriteChannel;
import com.knu.sosuso.capstone.domain.scrap.entity.Scrap;
import com.knu.sosuso.capstone.domain.channel.repository.FavoriteChannelRepository;
import com.knu.sosuso.capstone.domain.scrap.repository.ScrapRepository;
import com.knu.sosuso.capstone.global.security.jwt.JwtUtil;
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
        log.info("🔍 [UserData] getUserFavoriteChannelId 호출 - apiChannelId={}", apiChannelId);

        if (token == null || !jwtUtil.isValidToken(token)) {
            log.warn("🔍 [UserData] 토큰 없음 또는 유효하지 않음");
            return null;
        }

        try {
            Long userId = jwtUtil.getUserId(token);
            log.info("🔍 [UserData] userId={}, apiChannelId={}", userId, apiChannelId);

            Optional<FavoriteChannel> favoriteChannel = favoriteChannelRepository.findByUserIdAndApiChannelId(userId, apiChannelId);
            Long result = favoriteChannel.map(FavoriteChannel::getId).orElse(null);

            log.info("🔍 [UserData] 조회 결과 - favoriteChannelId={}, 존재여부={}", result, favoriteChannel.isPresent());
            return result;

        } catch (Exception e) {
            log.warn("관심 채널 ID 조회 실패: apiChannelId={}, error={}", apiChannelId, e.getMessage());
            return null;
        }
    }
}