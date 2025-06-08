package com.knu.sosuso.capstone.swagger;

import com.knu.sosuso.capstone.dto.ResponseDto;
import com.knu.sosuso.capstone.dto.request.CreateScrapRequest;
import com.knu.sosuso.capstone.dto.response.CreateScrapResponse;
import com.knu.sosuso.capstone.dto.response.LoginResponse;
import com.knu.sosuso.capstone.swagger.annotation.ErrorCode400;
import com.knu.sosuso.capstone.swagger.annotation.ErrorCode500;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "스크랩 API")
public interface ScrapControllerSwagger {

    @Operation(
            summary = "스크랩 생성",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "스크랩 생성 성공",
                            content = @Content(schema = @Schema(implementation = CreateScrapResponse.class))
                    )
            }
    )
    @ErrorCode400
    @ErrorCode500
    ResponseDto<CreateScrapResponse> createScrap(
            @CookieValue("Authorization") String token,
            @RequestBody @Valid CreateScrapRequest createScrapRequest
    );

    @Operation(
            summary = "스크랩 취소",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "스크랩 취소 성공"
                    )
            }
    )
    @Parameter(name = "id", description = "scrapId", required = true)
    @ErrorCode400
    @ErrorCode500
    ResponseDto<?> cancelScrap(
            @CookieValue("Authorization") String token,
            @PathVariable("id") Long scrapId
    );
}
