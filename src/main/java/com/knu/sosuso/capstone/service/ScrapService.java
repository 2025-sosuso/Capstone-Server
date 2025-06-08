package com.knu.sosuso.capstone.service;

import com.knu.sosuso.capstone.domain.Scrap;
import com.knu.sosuso.capstone.domain.User;
import com.knu.sosuso.capstone.domain.Video;
import com.knu.sosuso.capstone.dto.request.CreateScrapRequest;
import com.knu.sosuso.capstone.dto.response.CreateScrapResponse;
import com.knu.sosuso.capstone.exception.BusinessException;
import com.knu.sosuso.capstone.exception.error.AuthenticationError;
import com.knu.sosuso.capstone.exception.error.ScrapError;
import com.knu.sosuso.capstone.exception.error.VideoError;
import com.knu.sosuso.capstone.repository.ScrapRepository;
import com.knu.sosuso.capstone.repository.UserRepository;
import com.knu.sosuso.capstone.repository.VideoRepository;
import com.knu.sosuso.capstone.security.jwt.JwtUtil;
import jakarta.persistence.Entity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ScrapService {

    private final ScrapRepository scrapRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public CreateScrapResponse createScrap(String token, CreateScrapRequest createScrapRequest) {
        if (!jwtUtil.isValidToken(token)) {
            throw new BusinessException(AuthenticationError.INVALID_TOKEN);
        }

        Long userId = jwtUtil.getUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(AuthenticationError.USER_NOT_FOUND));

        String apiVideoId = createScrapRequest.apiVideoId();
        Video video = videoRepository.findByApiVideoId(apiVideoId)
                .orElseThrow(() -> new BusinessException(VideoError.VIDEO_NOT_FOUND));

        boolean existsScrap = scrapRepository.existsByUserIdAndApiVideoId(userId, apiVideoId);
        if (existsScrap) {
            throw new BusinessException(ScrapError.SCRAP_ALREADY_EXISTS);
        }

        Scrap scrap = Scrap.builder()
                .user(user)
                .video(video)
                .apiVideoId(apiVideoId)
                .build();

        scrapRepository.save(scrap);

        return new CreateScrapResponse(
                scrap.getId()
        );
    }

    // Todo custom exception으로 바꾸기
    @Transactional
    public void cancelScrap(String token, Long scrapId) {
        if (!jwtUtil.isValidToken(token)) {
            throw new BusinessException(AuthenticationError.INVALID_TOKEN);
        }

        Scrap scrap = scrapRepository.findById(scrapId)
                .orElseThrow(() -> new BusinessException(ScrapError.SCRAP_NOT_FOUNT));

        Long userId = jwtUtil.getUserId(token);
        if (!scrap.getUser().getId().equals(userId)) {
            throw new BusinessException(ScrapError.FORBIDDEN_SCRAP_DELETE);
        }

        scrapRepository.deleteById(scrapId);
    }
}
