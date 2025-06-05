package com.knu.sosuso.capstone.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.sosuso.capstone.jwt.JwtUtil;
import com.knu.sosuso.capstone.dto.oauth2.CustomOAuth2User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@RequiredArgsConstructor
@Component
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        CustomOAuth2User customUserDetails = (CustomOAuth2User) authentication.getPrincipal();
        String sub = customUserDetails.getSub();

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String role = authorities.iterator().next().getAuthority();

        // JWT 생성 (1시간)
        String token = jwtUtil.createJwt(sub, role, 60 * 60 * 1000L);

        ResponseCookie cookie = ResponseCookie.from("Authorization", token)
                .httpOnly(true)
                .secure(false) // Todo 배포 시 true로 변경
                .sameSite("None")
                .path("/")
                .maxAge(60 * 60)
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        response.sendRedirect("http://localhost:3000/login/success");
    }
}