package com.knu.sosuso.capstone.swagger;

import com.knu.sosuso.capstone.dto.ResponseDto;
import com.knu.sosuso.capstone.dto.request.RegisterFavoriteChannelRequest;
import com.knu.sosuso.capstone.dto.response.favorite_channel.CancelFavoriteChannelResponse;
import com.knu.sosuso.capstone.dto.response.favorite_channel.RegisterFavoriteChannelResponse;
import com.knu.sosuso.capstone.swagger.annotation.ErrorCode400;
import com.knu.sosuso.capstone.swagger.annotation.ErrorCode500;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "관심 채널 API")
public interface FavoriteChannelControllerSwagger {

    @Operation(
            summary = "관심 채널 등록",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "관심 채널 등록 성공",
                            content = @Content(schema = @Schema(implementation = RegisterFavoriteChannelResponse.class))
                    )
            }
    )
    @ErrorCode400
    @ErrorCode500
    ResponseDto<RegisterFavoriteChannelResponse> registerFavoriteChannel(
            @CookieValue("Authorization") String token,
            @RequestBody RegisterFavoriteChannelRequest registerFavoriteChannelRequest
    );

    @Operation(
            summary = "관심 채널 등록 취소",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "관심 채널 등록 취소 성공"
                    )
            }
    )
    @Parameter(name = "id", description = "channelId", required = true)
    @ErrorCode400
    @ErrorCode500
    ResponseDto<CancelFavoriteChannelResponse> cancelFavoriteChannel(
            @CookieValue("Authorization") String token,
            @PathVariable("id") Long channelId
    );
}
