package com.knu.sosuso.capstone.domain.channel.repository;

import com.knu.sosuso.capstone.domain.channel.entity.FavoriteChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteChannelRepository extends JpaRepository<FavoriteChannel, Long> {
    boolean existsByUserIdAndApiChannelId(Long userId, String apiChannelId);
    Optional<FavoriteChannel> findByUserIdAndApiChannelId(Long userId, String apiChannelId);
    List<FavoriteChannel> findByUserId(Long userId);
    Optional<FavoriteChannel> findByIdAndUserId(Long favoriteChannelId, Long userId);
}
