package com.knu.sosuso.capstone.dto;

import com.knu.sosuso.capstone.dto.request.GoogleUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User {

    private final GoogleUserInfo googleUserInfo;

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

    public String getEmail() {
        return googleUserInfo.email();
    }

    public String getPicture() {
        return googleUserInfo.picture();
    }

}
