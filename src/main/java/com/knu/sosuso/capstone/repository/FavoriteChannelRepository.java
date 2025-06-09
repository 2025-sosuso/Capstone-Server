package com.knu.sosuso.capstone.repository;

import com.knu.sosuso.capstone.domain.FavoriteChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FavoriteChannelRepository extends JpaRepository<FavoriteChannel, Long> {
    boolean existsByUserIdAndApiChannelId(Long userId, String apiChannelId);
    Optional<FavoriteChannel> findByUserIdAndApiChannelId(Long userId, String apiChannelId);
}
