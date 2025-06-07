package com.knu.sosuso.capstone.repository;

import com.knu.sosuso.capstone.domain.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VideoRepository extends JpaRepository<Video, Long>  {
    Optional<Video> findByApiVideoId(String apiVideoId);

}
