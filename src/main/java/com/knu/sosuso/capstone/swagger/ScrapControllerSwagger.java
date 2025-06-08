package com.knu.sosuso.capstone.swagger;

import com.knu.sosuso.capstone.dto.ResponseDto;
import com.knu.sosuso.capstone.dto.request.CreateScrapRequest;
import com.knu.sosuso.capstone.dto.response.CreateScrapResponse;
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
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(
        name = "스크랩 API",
        description = "스크랩 기능에 대한 API입니다. " +
                "관심 영상을 스크랩으로 저장, 취소, 조회할 수 있습니다."
)
public interface ScrapControllerSwagger {

    @Operation(
            summary = "스크랩 생성",
            description = "관심 있는 영상을 스크랩합니다. " +
                    "이미 스크랩된 영상의 경우 중복 스크랩이 방지되며, " +
                    "영상이 DB에 존재하지 않는 경우 에러가 발생합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "스크랩 생성 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseDto.class),
                                    examples = @ExampleObject(
                                            name = "스크랩 생성 성공 응답",
                                            summary = "정상적으로 스크랩이 생성된 경우",
                                            value = """
                                                    {
                                                        "timeStamp": "2025-06-09T03:00:17.5543788",
                                                        "message": "Successfully created Scrap",
                                                        "data": {
                                                            "scrapId": 14
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
                            responseCode = "404",
                            description = "영상을 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "영상 없음 에러",
                                            value = """
                                                    {
                                                        "httpStatus": "NOT_FOUND",
                                                        "message": "존재하지 않는 영상입니다.",
                                                        "timeStamp": "2025-06-09T02:31:29.5376518"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "중복된 스크랩 - 이미 스크랩된 영상",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "중복 스크랩 에러",
                                            value = """
                                                    {
                                                        "httpStatus": "CONFLICT",
                                                        "message": "이미 스크랩된 영상입니다.",
                                                        "timeStamp": "2025-06-09T02:31:36.7675238"
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
            description = "스크랩 생성을 위한 요청 데이터",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CreateScrapRequest.class),
                    examples = @ExampleObject(
                            name = "스크랩 생성 요청",
                            summary = "YouTube API Video ID를 통한 스크랩 생성",
                            value = """
                                    {
                                        "apiVideoId": "dQw4w9WgXcQ"
                                    }
                                    """,
                            description = "apiVideoId는 YouTube API에서 제공하는 영상의 고유 식별자입니다."
                    )
            )
    )
    @ErrorCode400
    @ErrorCode500
    ResponseDto<CreateScrapResponse> createScrap(
            @CookieValue("Authorization") String token,
            @org.springframework.web.bind.annotation.RequestBody @Valid CreateScrapRequest createScrapRequest
    );

    @Operation(
            summary = "스크랩 취소",
            description = "지정된 ID의 스크랩을 삭제합니다. " +
                    "삭제된 스크랩은 복구할 수 없으며, 본인의 스크랩만 삭제할 수 있습니다. " +
                    "존재하지 않는 스크랩이거나, 다른 사용자의 스크랩인 경우 에러가 발생합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "스크랩 취소 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseDto.class),
                                    examples = @ExampleObject(
                                            name = "스크랩 취소 성공 응답",
                                            summary = "정상적으로 스크랩이 취소된 경우",
                                            value = """
                                                    {
                                                        "timeStamp": "2025-06-09T03:02:32.7160173",
                                                        "message": "Successfully canceled the scrap.",
                                                        "data": null
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
                                    examples = @ExampleObject(
                                            name = "인증 실패 에러",
                                            value = """
                                                    {
                                                        "httpStatus": "UNAUTHORIZED",
                                                        "message": "유효하지 않은 토큰입니다.",
                                                        "timeStamp": "2025-06-09T02:31:14.5927009"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "권한 없음 - 다른 사용자의 스크랩",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "권한 없음 에러",
                                            value = """
                                                    {
                                                        "httpStatus": "FORBIDDEN",
                                                        "message": "본인의 스크랩만 취소할 수 있습니다.",
                                                        "timeStamp": "2025-06-09T02:31:36.7675238"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "스크랩을 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "스크랩 없음 에러",
                                            value = """
                                                    {
                                                        "httpStatus": "NOT_FOUND",
                                                        "message": "존재하지 않는 스크랩입니다.",
                                                        "timeStamp": "2025-06-09T02:31:36.7675238"
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
                    description = "scrapId",
                    required = true,
                    in = ParameterIn.PATH,
                    schema = @Schema(type = "integer", format = "int64", minimum = "1"),
                    example = "12345"
            )
    })
    @ErrorCode400
    @ErrorCode500
    ResponseDto<?> cancelScrap(
            @CookieValue("Authorization") String token,
            @PathVariable("id") Long scrapId
    );
}