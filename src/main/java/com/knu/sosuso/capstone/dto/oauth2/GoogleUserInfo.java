package com.knu.sosuso.capstone.dto.oauth2;

public record GoogleUserInfo(
        String sub,
        String email,
        String name,
        String role,
        String picture
) {
}
