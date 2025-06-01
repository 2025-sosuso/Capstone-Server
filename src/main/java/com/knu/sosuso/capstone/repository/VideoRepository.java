package com.knu.sosuso.capstone.repository;

import com.knu.sosuso.capstone.domain.Video;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<Video, Long>  {
}
