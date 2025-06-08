package com.knu.sosuso.capstone.repository;

import com.knu.sosuso.capstone.domain.Scrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScrapRepository extends JpaRepository<Scrap, Long> {
    boolean existsByUserIdAndApiVideoId(Long userId, String apiVideoId);
}
