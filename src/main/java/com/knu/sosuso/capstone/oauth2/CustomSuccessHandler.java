package com.knu.sosuso.capstone.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.sosuso.capstone.dto.response.LoginResponse;
import com.knu.sosuso.capstone.jwt.JwtUtil;
import com.knu.sosuso.capstone.dto.CustomOAuth2User;
import io.jsonwebtoken.io.IOException;
import io.jsonwebtoken.io.SerialException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Iterator;

@RequiredArgsConstructor
@Component
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, SerialException, java.io.IOException {
        CustomOAuth2User customUserDetails = (CustomOAuth2User) authentication.getPrincipal();

        String username = customUserDetails.getSub();

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();
        String role = auth.getAuthority();

        String token = jwtUtil.createJwt(username, role, 60*60*60L);
        response.addCookie(createCookie("Authorization", token));

        String name = jwtUtil.getName(token);
        String email = jwtUtil.getEmail(token);
        String profileImage = jwtUtil.getPicture(token);
        LoginResponse loginResponse = new LoginResponse(name, email, profileImage);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), loginResponse);
    }

    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(60*60*60);
//        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setHttpOnly(true);

        return cookie;
    }
}
