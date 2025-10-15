package com.knu.sosuso.capstone.domain.auth;

import com.knu.sosuso.capstone.global.ResponseDto;
import com.knu.sosuso.capstone.global.security.CustomSuccessHandler;
import com.knu.sosuso.capstone.global.swagger.AuthControllerSwagger;
import jakarta.servlet.http.HttpServletRequest;
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
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        customSuccessHandler.logout(token, request, response);
        return ResponseDto.of("Successfully Logged out.");
    }
}