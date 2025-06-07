package com.knu.sosuso.capstone.swagger;

import com.knu.sosuso.capstone.dto.ResponseDto;
import com.knu.sosuso.capstone.dto.response.LoginResponse;
import com.knu.sosuso.capstone.swagger.annotation.ErrorCode400;
import com.knu.sosuso.capstone.swagger.annotation.ErrorCode500;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.CookieValue;

import java.io.IOException;

@Tag(name = "구글 소셜 로그인 API")
public interface AuthControllerSwagger {

    @Operation(
            summary = "구글 로그인 성공 시 회원 정보 불러오기",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "회원 정보 불러오기 성공",
                            content = @Content(schema = @Schema(implementation = LoginResponse.class))
                    )
            }
    )
    @ErrorCode400
    @ErrorCode500
    ResponseDto<LoginResponse> googleLogin(
            @CookieValue("Authorization") String token
    );

    @Operation(
            summary = "구글 소셜 로그아웃",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "구글 소셜 로그아웃 성공"
                    )
            }
    )
    @ErrorCode400
    @ErrorCode500
    ResponseDto<?> googleLogout(
            @CookieValue(value = "Authorization", required = false) String token,
            HttpServletResponse response
    ) throws IOException;
}
