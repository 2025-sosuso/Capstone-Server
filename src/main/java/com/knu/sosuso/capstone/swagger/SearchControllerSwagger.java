package com.knu.sosuso.capstone.swagger;

import com.knu.sosuso.capstone.dto.ResponseDto;
import com.knu.sosuso.capstone.dto.response.search.SearchApiResponse;
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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(
        name = "통합 검색 API",
        description = "YouTube 영상 URL 및 채널 검색 API입니다. " +
                "YouTube URL을 입력하면 영상 분석 결과를, 검색어를 입력하면 채널 검색 결과를 반환합니다. " +
                "로그인한 사용자는 스크랩/관심채널 정보가 포함됩니다."
)
public interface SearchControllerSwagger {
    @Operation(
            summary = "통합 검색",
            description = "YouTube URL 또는 검색어를 통한 통합 검색 기능입니다.\n\n" +
                    "**검색 타입:**\n" +
                    "- **YouTube URL 입력**: 영상 분석 결과 반환 (댓글 분석, AI 요약, 감정 분석 등)\n" +
                    "- **일반 검색어 입력**: 채널 검색 결과 반환\n\n" +
                    "**지원하는 YouTube URL 형식:**\n" +
                    "- `https://www.youtube.com/watch?v=VIDEO_ID`\n" +
                    "- `https://youtu.be/VIDEO_ID`\n\n" +
                    "**로그인 사용자 혜택:**\n" +
                    "- 스크랩 여부 및 스크랩 ID 정보 제공\n" +
                    "- 관심 채널 여부 및 관심 채널 ID 정보 제공",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "검색 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseDto.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "YouTube URL 검색 성공 (로그인)",
                                                    summary = "로그인한 사용자의 YouTube 영상 분석 결과",
                                                    value = """
                                                            {
                                                                "timeStamp": "2025-06-09T15:30:00",
                                                                "message": "검색이 완료되었습니다.",
                                                                "data": {
                                                                    "searchType": "URL",
                                                                    "results": [{
                                                                        "video": {
                                                                            "id": "dQw4w9WgXcQ",
                                                                            "title": "Rick Astley - Never Gonna Give You Up",
                                                                            "description": "Official music video...",
                                                                            "publishedAt": "2009-10-25T06:57:33Z",
                                                                            "thumbnailUrl": "https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
                                                                            "viewCount": 1500000000,
                                                                            "likeCount": 15000000,
                                                                            "commentCount": 2000000,
                                                                            "scrapId": 123
                                                                        },
                                                                        "channel": {
                                                                            "id": "UCuAXFkgsw1L7xaCfnd5JJOw",
                                                                            "title": "Rick Astley",
                                                                            "thumbnailUrl": "https://yt3.ggpht.com/...",
                                                                            "subscriberCount": 3500000,
                                                                            "favoriteChannelId": 456
                                                                        },
                                                                        "analysis": {
                                                                            "summary": "이 영상은 Rick Astley의 대표곡으로...",
                                                                            "isWarning": false,
                                                                            "topComments": [...],
                                                                            "languageDistribution": [...],
                                                                            "sentimentDistribution": {...},
                                                                            "popularTimestamps": [...],
                                                                            "commentHistogram": [...],
                                                                            "keywords": [...]
                                                                        },
                                                                        "comments": [...]
                                                                    }]
                                                                }
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "채널 검색 성공 (비로그인)",
                                                    summary = "비로그인 사용자의 채널 검색 결과",
                                                    value = """
                                                            {
                                                                "timeStamp": "2025-06-09T15:30:00",
                                                                "message": "검색이 완료되었습니다.",
                                                                "data": {
                                                                    "searchType": "CHANNEL",
                                                                    "results": [
                                                                        {
                                                                            "id": "UCmGSJVG3mCRXVOP4yZrU1Dw",
                                                                            "title": "Troye Sivan",
                                                                            "handle": "@troyesivan",
                                                                            "description": "Official YouTube channel...",
                                                                            "thumbnailUrl": "https://yt3.ggpht.com/...",
                                                                            "subscriberCount": 7890000,
                                                                            "favoriteChannelId": null
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
                            description = "잘못된 요청 - 검색어 누락 또는 잘못된 YouTube URL",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "잘못된 요청 에러",
                                            value = """
                                                    {
                                                        "httpStatus": "BAD_REQUEST",
                                                        "message": "잘못된 요청: 검색어는 필수입니다",
                                                        "timeStamp": "2025-06-09T15:30:00"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "검색 결과 없음 또는 존재하지 않는 YouTube 영상",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "검색 결과 없음 에러",
                                            value = """
                                                    {
                                                        "httpStatus": "NOT_FOUND",
                                                        "message": "유효하지 않은 YouTube URL입니다",
                                                        "timeStamp": "2025-06-09T15:30:00"
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
                    description = "JWT 토큰 (Cookie) - 선택사항",
                    required = false,
                    in = ParameterIn.COOKIE,
                    schema = @Schema(type = "string", format = "jwt"),
                    example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
            ),
            @Parameter(
                    name = "query",
                    description = "검색어 또는 YouTube URL",
                    required = true,
                    in = ParameterIn.QUERY,
                    schema = @Schema(type = "string"),
                    examples = {
                            @ExampleObject(
                                    name = "YouTube URL",
                                    value = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                                    description = "YouTube 영상 URL을 입력하면 영상 분석 결과를 반환합니다"
                            ),
                            @ExampleObject(
                                    name = "채널 검색어",
                                    value = "아이유",
                                    description = "채널명을 검색하면 관련 채널 목록을 반환합니다"
                            )
                    }
            )
    })
    @ErrorCode400
    @ErrorCode500
    ResponseEntity<ResponseDto<SearchApiResponse<?>>> search(
            @CookieValue(value = "Authorization", required = false) String token,
            @RequestParam String query
    );
}

