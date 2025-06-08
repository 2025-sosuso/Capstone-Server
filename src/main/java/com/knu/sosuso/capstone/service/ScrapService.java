package com.knu.sosuso.capstone.service;

import com.knu.sosuso.capstone.domain.Scrap;
import com.knu.sosuso.capstone.domain.User;
import com.knu.sosuso.capstone.domain.Video;
import com.knu.sosuso.capstone.dto.request.CreateScrapRequest;
import com.knu.sosuso.capstone.dto.response.CreateScrapResponse;
import com.knu.sosuso.capstone.repository.ScrapRepository;
import com.knu.sosuso.capstone.repository.UserRepository;
import com.knu.sosuso.capstone.repository.VideoRepository;
import com.knu.sosuso.capstone.security.jwt.JwtUtil;
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
            throw new RuntimeException("유효하지 않은 토큰입니다."); // Todo custom exception으로 바꾸기
        }

        Long userId = jwtUtil.getUserId(token);
        User user = userRepository.findById(userId).orElseThrow(); // Todo custom exception으로 바꾸기
        String apiVideoId = createScrapRequest.apiVideoId();
        Video video = videoRepository.findByApiVideoId(apiVideoId).orElseThrow();

        boolean existsScrap = scrapRepository.existsByUserIdAndApiVideoId(userId, apiVideoId);
        if (existsScrap) {
            throw new RuntimeException("이미 스크랩된 영상입니다."); // Todo custom exception으로 바꾸기
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
            throw new RuntimeException("유효하지 않은 토큰입니다.");
        }

        Scrap scrap = scrapRepository.findById(scrapId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 스크랩입니다."));

        Long userId = jwtUtil.getUserId(token);
        if (!scrap.getUser().getId().equals(userId)) {
            throw new RuntimeException("본인의 스크랩만 취소할 수 있습니다.");
        }

        scrapRepository.deleteById(scrapId);
    }
}
