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
import org.springframework.web.bind.annotation.CookieValue;

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
}
