package com.knu.sosuso.capstone.jwt;

import com.knu.sosuso.capstone.domain.User;
import com.knu.sosuso.capstone.dto.oauth2.CustomOAuth2User;
import com.knu.sosuso.capstone.dto.oauth2.GoogleUserInfo;
import com.knu.sosuso.capstone.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    private static final String[] WHITELIST_PATHS = {
            "/swagger-ui", "/v3/api-docs", "/swagger-ui.html", "/oauth2", "/login"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (isWhitelistPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 쿠키에서 JWT 토큰 추출
        String token = extractTokenFromCookies(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtUtil.isValidToken(token)) {
            clearAuthCookie(response);
            filterChain.doFilter(request, response);
            return;
        }

        String sub = jwtUtil.getSub(token);
        String role = jwtUtil.getRole(token);

        User user = userRepository.findBySub(sub);
        if (user == null) {
            clearAuthCookie(response);
            filterChain.doFilter(request, response);
            return;
        }

        setAuthentication(user, role);
        filterChain.doFilter(request, response);
    }

    /**
     * 화이트리스트 경로 확인
     * @param path
     * @return
     */
    private boolean isWhitelistPath(String path) {
        for (String whitelistPath : WHITELIST_PATHS) {
            if (path.startsWith(whitelistPath)) {
                return true;
            }
        }
        return false;
    }

    private String extractTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if ("Authorization".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * DB에 저장되어있는 정보로 인증 객체 설정
     * @param user
     * @param role
     */
    private void setAuthentication(User user, String role) {
        // DB에서 최신 정보를 가져온다.
        GoogleUserInfo googleUserInfo = new GoogleUserInfo(
                user.getSub(),
                user.getEmail(),
                user.getName(),
                role,
                user.getPicture()
        );

        CustomOAuth2User customOAuth2User = new CustomOAuth2User(googleUserInfo);

        // 인증 토큰 생성 및 SecurityContext에 설정한다.
        Authentication authToken = new UsernamePasswordAuthenticationToken(
                customOAuth2User, null, customOAuth2User.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    /**
     * 인증 쿠키 삭제 (토큰 만료시)
     * @param response
     */
    private void clearAuthCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("Authorization", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }
}
