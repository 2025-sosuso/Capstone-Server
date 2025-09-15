package com.knu.sosuso.capstone.domain.auth;

public record LoginResponse(
        String userName,
        String userEmail,
        String userProfileImage
) {
}
