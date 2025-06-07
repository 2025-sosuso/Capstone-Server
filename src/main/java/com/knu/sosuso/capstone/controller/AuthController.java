package com.knu.sosuso.capstone.controller;

import com.knu.sosuso.capstone.dto.ResponseDto;
import com.knu.sosuso.capstone.dto.response.LoginResponse;
import com.knu.sosuso.capstone.security.CustomSuccessHandler;
import com.knu.sosuso.capstone.service.AuthService;
import com.knu.sosuso.capstone.swagger.AuthControllerSwagger;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RequiredArgsConstructor
@RequestMapping("/api/auth")
@RestController
public class AuthController implements AuthControllerSwagger {

    private final AuthService authService;
    private final CustomSuccessHandler customSuccessHandler;

    @GetMapping("/login")
    public ResponseDto<LoginResponse> googleLogin(
            @CookieValue("Authorization") String token
    ) {
        LoginResponse loginResponse = authService.getUserInformation(token);
        return ResponseDto.of(loginResponse, "Successfully signed in to Google Social.");
    }

    @PostMapping("/logout")
    public ResponseDto<?> googleLogout(
            @CookieValue(value = "Authorization", required = false) String token,
            HttpServletResponse response
    ) throws IOException {
        customSuccessHandler.logout(token, response);
        return ResponseDto.of("Successfully Logged out.");
    }
}
