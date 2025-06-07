package com.knu.sosuso.capstone.controller;

import com.knu.sosuso.capstone.dto.ResponseDto;
import com.knu.sosuso.capstone.dto.response.LoginResponse;
import com.knu.sosuso.capstone.service.AuthService;
import com.knu.sosuso.capstone.swagger.AuthControllerSwagger;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/auth")
@RestController
public class AuthController implements AuthControllerSwagger {

    private final AuthService authService;

    @GetMapping("/login")
    public ResponseDto<LoginResponse> googleLogin(
            @CookieValue("Authorization") String token
    ) {
        LoginResponse loginResponse = authService.getUserInformation(token);
        return ResponseDto.of(loginResponse, "Successfully signed in to Google Social.");
    }
}
