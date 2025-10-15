package com.knu.sosuso.capstone.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class OAuth2RedirectCaptureFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        if (requestURI.startsWith("/oauth2/authorization/")) {
            String redirectUri = request.getParameter("redirect_uri");

            if (redirectUri != null && !redirectUri.isEmpty()) {
                ResponseCookie cookie = ResponseCookie.from("REDIRECT_URI", redirectUri)
                        .httpOnly(true)
                        .secure(true)
                        .sameSite("None")
                        .path("/")
                        .maxAge(300)
                        .build();

                response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
                log.info("redirect_uri 저장: {}", redirectUri);
            }
        }

        filterChain.doFilter(request, response);
    }
}