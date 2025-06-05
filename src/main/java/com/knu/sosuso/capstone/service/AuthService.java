package com.knu.sosuso.capstone.service;

import com.knu.sosuso.capstone.domain.User;
import com.knu.sosuso.capstone.dto.oauth2.CustomOAuth2User;
import com.knu.sosuso.capstone.dto.response.LoginResponse;
import com.knu.sosuso.capstone.jwt.JwtAuthHelper;
import com.knu.sosuso.capstone.jwt.JwtUtil;
import com.knu.sosuso.capstone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AuthService {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public LoginResponse getUserInformation(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new RuntimeException("토큰이 없습니다. 로그인이 필요합니다.");
        }

        if (!jwtUtil.isValidToken(token)) {
            throw new RuntimeException("유효하지 않거나, 만료된 토큰입니다.");
        }

        String sub = jwtUtil.getSub(token);

        User user = userRepository.findBySub(sub);
        if (user == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }

        return new LoginResponse(
                user.getName(),
                user.getEmail(),
                user.getPicture()
        );
    }
    
    public boolean isTokenValid(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        return jwtUtil.isValidToken(token);
    }
}
