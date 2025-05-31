package com.knu.sosuso.capstone.dto.response;

public record LoginResponse(
        String userName,
        String userEmail,
        String userProfileImage
) {
}
