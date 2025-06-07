package com.knu.sosuso.capstone.dto.oauth2;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {

    @Getter
    private final Long userId;
    private final GoogleUserInfo googleUserInfo;

    public CustomOAuth2User(GoogleUserInfo googleUserInfo) {
        this.googleUserInfo = googleUserInfo;
        this.userId = null;
    }

    public CustomOAuth2User(GoogleUserInfo googleUserInfo, Long userId) {
        this.googleUserInfo = googleUserInfo;
        this.userId = userId;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> collection = new ArrayList<>();

        collection.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                return googleUserInfo.role();
            }
        });
        return collection;
    }

    @Override
    public String getName() {
        return googleUserInfo.name();
    }

    public String getSub() {
        return googleUserInfo.sub();
    }

}
