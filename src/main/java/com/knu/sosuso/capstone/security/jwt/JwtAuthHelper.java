package com.knu.sosuso.capstone.security.jwt;

import com.knu.sosuso.capstone.security.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class JwtAuthHelper {

    /**
     * 현재 인증된 사용자 정보 반환
     * @return
     */
    public CustomOAuth2User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        try {
            return (CustomOAuth2User) authentication.getPrincipal();
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * 사용자 인증 여부 체크
     * @return
     */
    public boolean isAuthenticated() {
        return getCurrentUser() != null;
    }

    public CustomOAuth2User getAuthenticatedUser() {
        CustomOAuth2User user = getCurrentUser();
        if (user == null) {
            throw new RuntimeException("인증되지 않은 사용자입니다.");
        }
        return user;
    }

    /**
     * 현재 사용자의 role(권한) 반환
     * @return
     */
    public String getCurrentUserRole() {
        CustomOAuth2User user = getCurrentUser();
        if (user != null) {
            return user.getAuthorities().iterator().next().getAuthority();
        } else {
            return null;
        }
    }

    /**
     * 현재 사용자의 sub 반환
     * @return
     */
    public String getCurrentUserSub() {
        CustomOAuth2User user = getCurrentUser();
        if (user != null) {
            return user.getSub();
        } else {
            return null;
        }
    }

    /**
     * 현재 사용자의 userId 반환
     * @return
     */
    public Long getCurrentUserId() {
        CustomOAuth2User user = getCurrentUser();
        if (user != null) {
            return user.getUserId();
        } else {
            return null;
        }
    }
}
