package com.knu.sosuso.capstone.swagger;

import com.knu.sosuso.capstone.dto.ResponseDto;
import com.knu.sosuso.capstone.dto.request.RegisterFavoriteChannelRequest;
import com.knu.sosuso.capstone.dto.response.favorite_channel.CancelFavoriteChannelResponse;
import com.knu.sosuso.capstone.dto.response.favorite_channel.FavoriteChannelListResponse;
import com.knu.sosuso.capstone.dto.response.favorite_channel.RegisterFavoriteChannelResponse;
import com.knu.sosuso.capstone.exception.ErrorResponse;
import com.knu.sosuso.capstone.swagger.annotation.ErrorCode400;
import com.knu.sosuso.capstone.swagger.annotation.ErrorCode500;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Tag(
        name = "관심 채널 API",
        description = "관심 채널 기능에 대한 API입니다. " +
                "YouTube 채널을 관심 채널로 등록, 취소, 조회할 수 있습니다."
)
public interface FavoriteChannelControllerSwagger {

    @Operation(
            summary = "관심 채널 등록",
            description = "관심 있는 YouTube 채널을 관심 채널로 등록합니다. " +
                    "이미 등록된 채널의 경우 중복 등록이 방지되며, " +
                    "채널 ID와 채널명이 모두 필요합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "관심 채널 등록 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseDto.class),
                                    examples = @ExampleObject(
                                            name = "관심 채널 등록 성공 응답",
                                            summary = "정상적으로 관심 채널이 등록된 경우",
                                            value = """
                                                    {
                                                        "timeStamp": "2025-06-09T03:36:49.2833049",
                                                        "message": "Successfully registered the favorite channel.",
                                                        "data": {
                                                            "favoriteChannelId": 8,
                                                            "apiChannelId": "ac13"
                                                        }
                                                    }
                                            """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패 - 유효하지 않은 토큰",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "인증 실패 에러",
                                            value = """
                                            {
                                                "httpStatus": "UNAUTHORIZED",
                                                "message": "유효하지 않은 토큰입니다.",
                                                "timeStamp": "2025-06-09T10:30:00"
                                            }
                                            """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "중복된 관심 채널 - 이미 등록된 채널",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "중복 관심 채널 에러",
                                            value = """
                                            {
                                                "httpStatus": "CONFLICT",
                                                "message": "이미 등록된 관심 채널입니다.",
                                                "timeStamp": "2025-06-09T10:30:00"
                                            }
                                            """
                                    )
                            )
                    )
            },
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @Parameters({
            @Parameter(
                    name = "Authorization",
                    description = "JWT 토큰 (Cookie)",
                    required = true,
                    in = ParameterIn.COOKIE,
                    schema = @Schema(type = "string", format = "jwt"),
                    example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
            )
    })
    @RequestBody(
            description = "관심 채널 등록을 위한 요청 데이터",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = RegisterFavoriteChannelRequest.class),
                    examples = {
                            @ExampleObject(
                                    name = "관심 채널 등록 요청",
                                    value = """
                                    {
                                        "apiChannelId": "UCmGSJVG3mCRXVOP4yZrU1Dw",
                                        "apiChannelName": "Troye Sivan",
                                        "apiChannelThumbnail": "https://i.ytimg.com/vi/iPgt1tDN_So/sddefault.jpg"
                                    }
                                    """,
                                    description = "apiChannelId는 YouTube API Channel ID이고, apiChannelName은 채널의 표시명입니다"
                            )
                    }
            )
    )
    @ErrorCode400
    @ErrorCode500
    ResponseDto<RegisterFavoriteChannelResponse> registerFavoriteChannel(
            @CookieValue("Authorization") String token,
            @RequestBody @Valid RegisterFavoriteChannelRequest registerFavoriteChannelRequest
    );

    @Operation(
            summary = "관심 채널 리스트 조회",
            description = "사용자의 관심 채널 목록을 조회합니다. " +
                    "토큰을 통해 사용자를 식별하며, 등록된 관심 채널 목록을 리스트로 반환합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "관심 채널 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseDto.class),
                                    examples = @ExampleObject(
                                            name = "관심 채널 리스트 조회 성공 응답",
                                            summary = "정상적으로 관심 채널 리스트를 조회한 경우",
                                            value = """
                                                {
                                                    "timeStamp": "2025-06-09T12:00:00",
                                                    "message": "successfully retrieved your list of favorite channels.",
                                                    "data": [
                                                        {
                                                            "favoriteChannelId": 1,
                                                            "apiChannelName": "Troye Sivan",
                                                            "apiChannelThumbnail": "https://i.ytimg.com/vi/iPgt1tDN_So/sddefault.jpg"
                                                        },
                                                        {
                                                            "favoriteChannelId": 2,
                                                            "apiChannelName": "BLACKPINK",
                                                            "apiChannelThumbnail": "https://i.ytimg.com/vi/abcd1234/sddefault.jpg"
                                                        }
                                                    ]
                                                }
                                                """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패 - 유효하지 않은 토큰",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "인증 실패 에러",
                                            value = """
                                                {
                                                    "httpStatus": "UNAUTHORIZED",
                                                    "message": "유효하지 않은 토큰입니다.",
                                                    "timeStamp": "2025-06-09T10:30:00"
                                                }
                                                """
                                    )
                            )
                    )
            },
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @Parameter(
            name = "Authorization",
            description = "JWT 토큰 (Cookie)",
            required = true,
            in = ParameterIn.COOKIE,
            schema = @Schema(type = "string", format = "jwt"),
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    @ErrorCode400
    @ErrorCode500
    ResponseDto<List<FavoriteChannelListResponse>> favoriteChannelList(
            @CookieValue("Authorization") String token
    );


    @Operation(
            summary = "관심 채널 등록 취소",
            description = "지정된 ID의 관심 채널 등록을 취소합니다. " +
                    "삭제된 관심 채널은 복구할 수 없으며, 본인이 등록한 관심 채널만 취소 가능합니다. " +
                    "존재하지 않는 관심 채널이거나 다른 사용자의 관심 채널인 경우 에러가 발생합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "관심 채널 등록 취소 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseDto.class),
                                    examples = @ExampleObject(
                                            name = "관심 채널 취소 성공 응답",
                                            summary = "정상적으로 관심 채널 등록이 취소된 경우",
                                            value = """
                                                    {
                                                        "timeStamp": "2025-06-09T03:39:48.6976542",
                                                        "message": "successfully canceled the favorite channel.",
                                                        "data": {
                                                            "favoriteChannelId": 8
                                                        }
                                                    }
                                            """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패 - 유효하지 않은 토큰",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "인증 실패 에러",
                                            value = """
                                            {
                                                "httpStatus": "UNAUTHORIZED",
                                                "message": "유효하지 않은 토큰입니다.",
                                                "timeStamp": "2025-06-09T10:30:00"
                                            }
                                            """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "권한 없음 - 다른 사용자의 관심 채널",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "권한 없음 에러",
                                            value = """
                                            {
                                                "httpStatus": "FORBIDDEN",
                                                "message": "본인의 관심 채널만 취소할 수 있습니다.",
                                                "timeStamp": "2025-06-09T10:30:00"
                                            }
                                            """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "관심 채널을 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "관심 채널 없음 에러",
                                            value = """
                                            {
                                                "httpStatus": "NOT_FOUND",
                                                "message": "등록되어있지 않은 관심 채널입니다.",
                                                "timeStamp": "2025-06-09T10:30:00"
                                            }
                                            """
                                    )
                            )
                    )
            },
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @Parameters({
            @Parameter(
                    name = "Authorization",
                    description = "JWT 토큰 (Cookie)",
                    required = true,
                    in = ParameterIn.COOKIE,
                    schema = @Schema(type = "string", format = "jwt"),
                    example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
            ),
            @Parameter(
                    name = "id",
                    description = "favoriteChannelId",
                    required = true,
                    in = ParameterIn.PATH,
                    schema = @Schema(type = "integer", format = "int64", minimum = "1"),
                    example = "12345"
            )
    })
    @ErrorCode400
    @ErrorCode500
    ResponseDto<CancelFavoriteChannelResponse> cancelFavoriteChannel(
            @CookieValue("Authorization") String token,
            @PathVariable("id") Long channelId
    );
}