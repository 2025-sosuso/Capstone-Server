package com.knu.sosuso.capstone.dto;

import com.knu.sosuso.capstone.dto.request.AuthRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User {

    private final AuthRequest authRequest;

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
                return authRequest.role();
            }
        });
        return collection;
    }

    @Override
    public String getName() {
        return authRequest.name();
    }

    public String getSub() {
            return authRequest.sub();
    }
}
