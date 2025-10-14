package com.knu.sosuso.capstone.global.security;

import com.knu.sosuso.capstone.global.security.jwt.JwtUtil;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

@Slf4j
@RequiredArgsConstructor
@Component
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        CustomOAuth2User customUserDetails = (CustomOAuth2User) authentication.getPrincipal();
        String sub = customUserDetails.getSub();
        Long userId = customUserDetails.getUserId();

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String role = authorities.iterator().next().getAuthority();

        String token = jwtUtil.createJwt(sub, role, userId, 60 * 60 * 1000L);

        ResponseCookie cookie = ResponseCookie.from("Authorization", token)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(60 * 60)
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        String origin = request.getHeader("Origin");
        if (origin == null || origin.isEmpty()) {
            String referer = request.getHeader("Referer");
            if (referer != null) {
                origin = referer.substring(0, referer.indexOf("/", 8));
            }
        }

        String redirectUrl;
        if (origin != null && origin.startsWith("http://localhost:")) {
            redirectUrl = origin + "/login/success";
        } else {
            redirectUrl = "https://sosuso-client.vercel.app/login/success";
        }

        log.info("OAuth2 로그인 성공, 리다이렉트: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    public void logout(String token, HttpServletRequest request, HttpServletResponse response) throws IOException {
        clearAuthenticationCookie(response);
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        if (isValidToken(token)) {
            Long userId = jwtUtil.getUserId(token);
            log.info("OAuth2 로그아웃, 사용자 ID: {}", userId);

            String origin = request.getHeader("Origin");
            if (origin == null || origin.isEmpty()) {
                String referer = request.getHeader("Referer");
                if (referer != null) {
                    origin = referer.substring(0, referer.indexOf("/", 8));
                }
            }

            String googleLogoutUrl = buildGoogleLogoutUrl(origin);
            response.sendRedirect(googleLogoutUrl);
        }
    }

    private String buildGoogleLogoutUrl(String origin) {
        String logoutRedirectUrl;
        if (origin != null && origin.startsWith("http://localhost:")) {
            logoutRedirectUrl = origin;
        } else {
            logoutRedirectUrl = "https://sosuso-client.vercel.app";
        }

        log.info("구글 로그아웃 리다이렉트: {}", logoutRedirectUrl);
        return "https://accounts.google.com/logout?continue=" +
                URLEncoder.encode(logoutRedirectUrl, StandardCharsets.UTF_8);
    }

    /**
     * 토큰 유효성 검사
     */
    private boolean isValidToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        return !jwtUtil.isExpired(token);
    }

    /**
     * 쿠키 삭제
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
}