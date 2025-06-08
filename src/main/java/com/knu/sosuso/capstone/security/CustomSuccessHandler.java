package com.knu.sosuso.capstone.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.sosuso.capstone.security.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

@Slf4j
@RequiredArgsConstructor
@Component
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        CustomOAuth2User customUserDetails = (CustomOAuth2User) authentication.getPrincipal();
        String sub = customUserDetails.getSub();
        Long userId = customUserDetails.getUserId();

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String role = authorities.iterator().next().getAuthority();

        // JWT 생성 (1시간)
        String token = jwtUtil.createJwt(sub, role, userId, 60 * 60 * 1000L);

        ResponseCookie cookie = ResponseCookie.from("Authorization", token)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(60 * 60)
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        String referer = request.getHeader("Referer");
        String frontendUrl;

        if (referer != null) {
            if (referer.contains("localhost")) {
                frontendUrl = "http://localhost:3000";
            } else if (referer.contains("vercel.app")) {
                frontendUrl = "https://capstone-client-guka.vercel.app";
            } else {
                // 다른 도메인이면 기본값
                frontendUrl = "https://capstone-client-guka.vercel.app";
            }
        } else {
            // Referer도 없으면 기본값
            frontendUrl = "https://capstone-client-guka.vercel.app";
        }

        log.info("Referer 헤더: {}", referer);
        log.info("Frontend URL: {}", frontendUrl);

        response.sendRedirect(frontendUrl + "/login/success");
    }

    public void logout(String token, HttpServletResponse response) throws IOException {
        clearAuthenticationCookie(response);
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        if (isValidToken(token)) {
            Long userId = jwtUtil.getUserId(token);

            log.info("OAuth2 로그아웃, 사용자 ID: {}", userId);

            String googleLogoutUrl = buildGoogleLogoutUrl();
            response.sendRedirect(googleLogoutUrl);
        }
    }

    /**
     * 토큰 유효성 검사
     * @param token
     * @return
     */
    private boolean isValidToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        return !jwtUtil.isExpired(token);
    }

    /**
     * 쿠키 삭제
     * @param response
     */
    private void clearAuthenticationCookie(HttpServletResponse response) {
        ResponseCookie deleteCookie = ResponseCookie.from("Authorization", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(0)
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
    }

    private String buildGoogleLogoutUrl() {
        String logoutRedirectUrl = "https://capstone-client-guka.vercel.app";
        return "https://accounts.google.com/logout?continue=" +
                URLEncoder.encode(logoutRedirectUrl, StandardCharsets.UTF_8);
    }
}