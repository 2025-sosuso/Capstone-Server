package com.knu.sosuso.capstone.global.security;

import com.knu.sosuso.capstone.global.security.jwt.JwtUtil;
import jakarta.servlet.http.Cookie;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
@Component
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;

    private static final String DEFAULT_REDIRECT = "https://sosuso-client.vercel.app/login/success";

    private static final List<Pattern> ALLOWED_REDIRECT_PATTERNS = Arrays.asList(
            Pattern.compile("^https?://localhost:3000(/.*)?$"),
            Pattern.compile("^https://sosuso-client\\.vercel\\.app(/.*)?$"),
            Pattern.compile("^https://[a-z0-9-]+-sosuso-client\\.vercel\\.app(/.*)?$")
    );

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

        String targetUrl = getTargetRedirectUrl(request);
        removeRedirectCookie(response);

        log.info("OAuth2 로그인 성공, 리다이렉트: {}", targetUrl);
        response.sendRedirect(targetUrl);
    }

    private String getTargetRedirectUrl(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("REDIRECT_URI".equals(cookie.getName())) {
                    String redirectUri = cookie.getValue();

                    if (isAllowedRedirect(redirectUri)) {
                        log.info("허용된 redirect_uri 사용: {}", redirectUri);
                        return redirectUri;
                    } else {
                        log.warn("허용되지 않은 redirect_uri: {}, 기본값 사용", redirectUri);
                    }
                }
            }
        }

        log.info("redirect_uri 없음, 기본값 사용: {}", DEFAULT_REDIRECT);
        return DEFAULT_REDIRECT;
    }

    private boolean isAllowedRedirect(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        return ALLOWED_REDIRECT_PATTERNS.stream()
                .anyMatch(pattern -> pattern.matcher(url).matches());
    }

    private void removeRedirectCookie(HttpServletResponse response) {
        ResponseCookie deleteCookie = ResponseCookie.from("REDIRECT_URI", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
    }

    public void logout(String token, HttpServletRequest request, HttpServletResponse response) throws IOException {
        clearAuthenticationCookie(response);
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        if (isValidToken(token)) {
            Long userId = jwtUtil.getUserId(token);
            log.info("OAuth2 로그아웃, 사용자 ID: {}", userId);
        }
    }

    private boolean isValidToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        return !jwtUtil.isExpired(token);
    }

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