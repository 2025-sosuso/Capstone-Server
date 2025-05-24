package com.knu.sosuso.capstone.dto.request;

public record AuthRequest(
        String sub,
        String email,
        String name,
        String role,
        String picture
) {
}
