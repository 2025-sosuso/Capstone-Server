package com.knu.sosuso.capstone.security;

public record GoogleUserInfo(
        String sub,
        String email,
        String name,
        String role,
        String picture
) {
}
