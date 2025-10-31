package com.knu.sosuso.capstone.domain.auth;

import com.knu.sosuso.capstone.global.exception.BusinessException;
import com.knu.sosuso.capstone.global.exception.error.AuthenticationError;
import com.knu.sosuso.capstone.global.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AuthService {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public LoginResponse getUserInformation(String token) {
        validateToken(token);

        String sub = jwtUtil.getSub(token);
        User user = findUserBySub(sub);

        return new LoginResponse(
                user.getName(),
                user.getEmail(),
                user.getPicture()
        );
    }

    private void validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new BusinessException(AuthenticationError.TOKEN_NOT_FOUND);
        }

        if (!jwtUtil.isValidToken(token)) {
            throw new BusinessException(AuthenticationError.INVALID_TOKEN);
        }
    }

    private User findUserBySub(String sub) {
        User user = userRepository.findBySub(sub);
        if (user == null) {
            throw new BusinessException(AuthenticationError.USER_NOT_FOUND);
        }
        return user;
    }
}