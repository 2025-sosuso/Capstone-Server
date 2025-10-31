package com.knu.sosuso.capstone.global.swagger;

import com.knu.sosuso.capstone.global.ResponseDto;
import com.knu.sosuso.capstone.domain.video.dto.response.SearchApiResponse;
import com.knu.sosuso.capstone.global.exception.ErrorResponse;
import com.knu.sosuso.capstone.global.swagger.annotation.ErrorCode400;
import com.knu.sosuso.capstone.global.swagger.annotation.ErrorCode500;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(
        name = "통합 검색 API",
        description = "YouTube 영상 URL 및 채널 검색 API입니다. " +
                "YouTube URL을 입력하면 영상 ID를 반환하고, 검색어를 입력하면 채널 검색 결과를 반환합니다."
)
public interface SearchControllerSwagger {
    @Operation(
            summary = "통합 검색",
            description = "YouTube URL 또는 검색어를 통한 통합 검색 기능입니다.\n\n" +
                    "**검색 타입:**\n" +
                    "- **YouTube URL 입력**: apiVideoId 반환 → 프론트엔드가 상세 페이지(/videos/{apiVideoId})로 이동하여 분리된 API 호출\n" +
                    "  - `/api/videos/{apiVideoId}/basic` - 영상 기본 정보\n" +
                    "  - `/api/videos/{apiVideoId}/analysis` - 백엔드 분석 (히스토그램, 타임스탬프, TOP 댓글)\n" +
                    "  - `/api/videos/{apiVideoId}/ai` - AI 분석 (요약, 감정, 키워드)\n" +
                    "  - `/api/videos/{apiVideoId}/comments/all` - 전체 댓글\n" +
                    "- **일반 검색어 입력**: 채널 검색 결과 반환\n\n" +
                    "**지원하는 YouTube URL 형식:**\n" +
                    "- `https://www.youtube.com/watch?v=VIDEO_ID`\n" +
                    "- `https://youtu.be/VIDEO_ID`\n\n" +
                    "**장점:**\n" +
                    "- URL 검색 시 빠른 응답 (ID 추출만)\n" +
                    "- 상세 페이지에서 필요한 데이터만 로드\n" +
                    "- 효율적인 네트워크 사용",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "검색 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseDto.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "YouTube URL 검색",
                                                    summary = "YouTube URL로 검색 시 apiVideoId 반환",
                                                    value = """
                                                            {
                                                              "timeStamp": "2025-10-31T15:30:00",
                                                              "message": "영상 URL 검색이 완료되었습니다.",
                                                              "data": {
                                                                "searchType": "URL",
                                                                "results": [
                                                                  {
                                                                    "apiVideoId": "dQw4w9WgXcQ"
                                                                  }
                                                                ]
                                                              }
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "채널 검색 (비로그인)",
                                                    summary = "비로그인 사용자의 채널 검색 결과",
                                                    value = """
                                                            {
                                                                "timeStamp": "2025-10-31T15:30:00",
                                                                "message": "채널 검색이 완료되었습니다.",
                                                                "data": {
                                                                    "searchType": "CHANNEL",
                                                                    "results": [
                                                                        {
                                                                            "id": "UCmGSJVG3mCRXVOP4yZrU1Dw",
                                                                            "title": "채널명",
                                                                            "handle": "@channelhandle",
                                                                            "description": "채널 설명...",
                                                                            "thumbnailUrl": "https://yt3.ggpht.com/...",
                                                                            "subscriberCount": 1000000,
                                                                            "favoriteChannelId": null
                                                                        }
                                                                    ]
                                                                }
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "채널 검색 (로그인)",
                                                    summary = "로그인 사용자의 채널 검색 결과 - 관심 채널 정보 포함",
                                                    value = """
                                                            {
                                                                "timeStamp": "2025-10-31T15:30:00",
                                                                "message": "채널 검색이 완료되었습니다.",
                                                                "data": {
                                                                    "searchType": "CHANNEL",
                                                                    "results": [
                                                                        {
                                                                            "id": "UCmGSJVG3mCRXVOP4yZrU1Dw",
                                                                            "title": "채널명",
                                                                            "handle": "@channelhandle",
                                                                            "description": "채널 설명...",
                                                                            "thumbnailUrl": "https://yt3.ggpht.com/...",
                                                                            "subscriberCount": 1000000,
                                                                            "favoriteChannelId": 123
                                                                        }
                                                                    ]
                                                                }
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                        "httpStatus": "BAD_REQUEST",
                                                        "message": "잘못된 요청: 검색어는 필수입니다",
                                                        "timeStamp": "2025-10-31T15:30:00"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "서버 오류",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                        "httpStatus": "INTERNAL_SERVER_ERROR",
                                                        "message": "검색 중 오류 발생",
                                                        "timeStamp": "2025-10-31T15:30:00"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    @Parameters({
            @Parameter(
                    name = "Authorization",
                    description = "JWT 토큰 (선택사항) - 로그인 시 관심 채널 정보 포함",
                    required = false,
                    in = ParameterIn.COOKIE,
                    schema = @Schema(type = "string")
            ),
            @Parameter(
                    name = "query",
                    description = "YouTube URL 또는 채널 검색어",
                    required = true,
                    in = ParameterIn.QUERY,
                    schema = @Schema(type = "string"),
                    example = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
            )
    })
    @ErrorCode400
    @ErrorCode500
    ResponseEntity<ResponseDto<SearchApiResponse<?>>> search(
            @CookieValue(value = "Authorization", required = false) String token,
            @RequestParam String query
    );
}