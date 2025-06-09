package com.knu.sosuso.capstone.swagger;

import com.knu.sosuso.capstone.dto.ResponseDto;
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
        name = "인기급상승 영상 API",
        description = "YouTube 인기급상승 영상 조회 API입니다. " +
                "카테고리별 인기급상승 영상을 댓글 분석과 함께 제공합니다. " +
                "로그인한 사용자는 스크랩/관심채널 정보가 추가로 포함됩니다."
)
public interface TrendingControllerSwagger {
    @Operation(
            summary = "카테고리별 인기급상승 영상 조회",
            description = "지정된 카테고리의 인기급상승 YouTube 영상을 조회합니다.\n\n" +
                    "**지원 카테고리:**\n" +
                    "- `latest`: 전체 카테고리 (기본값)\n" +
                    "- `music`: 음악\n" +
                    "- `game`: 게임\n\n" +
                    "**제공 정보:**\n" +
                    "- 영상 기본 정보 (제목, 설명, 조회수, 좋아요 등)\n" +
                    "- 채널 정보 (채널명, 구독자 수 등)\n" +
                    "- 댓글 분석 결과 (감정 분석, 언어 분포, 인기 타임스탬프 등)\n" +
                    "- AI 요약 및 키워드\n\n" +
                    "**로그인 사용자 혜택:**\n" +
                    "- 스크랩 여부 및 스크랩 ID 정보 제공\n" +
                    "- 관심 채널 여부 및 관심 채널 ID 정보 제공\n\n" +
                    "**지역 설정:** 한국(KR) 기준으로 고정되어 있습니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "인기급상승 영상 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseDto.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "인기급상승 영상 조회 성공 (로그인)",
                                                    summary = "로그인한 사용자의 인기급상승 영상 조회 결과",
                                                    value = """
                                                            {
                                                                "timeStamp": "2025-06-09T15:30:00",
                                                                "message": "인기급상승 영상 조회 성공",
                                                                "data": [
                                                                    {
                                                                        "video": {
                                                                            "id": "abc123xyz",
                                                                            "title": "최신 인기 영상 제목",
                                                                            "description": "영상 설명...",
                                                                            "publishedAt": "2025-06-08T12:00:00Z",
                                                                            "thumbnailUrl": "https://i.ytimg.com/vi/abc123xyz/maxresdefault.jpg",
                                                                            "viewCount": 1500000,
                                                                            "likeCount": 85000,
                                                                            "commentCount": 12500,
                                                                            "scrapId": 789
                                                                        },
                                                                        "channel": {
                                                                            "id": "UC_channel_id",
                                                                            "title": "인기 채널",
                                                                            "thumbnailUrl": "https://yt3.ggpht.com/...",
                                                                            "subscriberCount": 2500000,
                                                                            "favoriteChannelId": 101
                                                                        },
                                                                        "analysis": {
                                                                            "summary": "이 영상은 최근 화제가 된...",
                                                                            "isWarning": false,
                                                                            "topComments": [...],
                                                                            "languageDistribution": [...],
                                                                            "sentimentDistribution": {
                                                                                "positive": 0.75,
                                                                                "negative": 0.15,
                                                                                "other": 0.10
                                                                            },
                                                                            "popularTimestamps": [...],
                                                                            "commentHistogram": [...],
                                                                            "keywords": ["키워드1", "키워드2", "키워드3"]
                                                                        },
                                                                        "comments": [...]
                                                                    }
                                                                ]
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "인기급상승 영상 조회 성공 (비로그인)",
                                                    summary = "비로그인 사용자의 인기급상승 영상 조회 결과",
                                                    value = """
                                                            {
                                                                "timeStamp": "2025-06-09T15:30:00",
                                                                "message": "인기급상승 영상 조회 성공",
                                                                "data": [
                                                                    {
                                                                        "video": {
                                                                            "id": "def456uvw",
                                                                            "title": "게임 인기 영상",
                                                                            "description": "최신 게임 플레이...",
                                                                            "publishedAt": "2025-06-08T18:30:00Z",
                                                                            "thumbnailUrl": "https://i.ytimg.com/vi/def456uvw/maxresdefault.jpg",
                                                                            "viewCount": 980000,
                                                                            "likeCount": 45000,
                                                                            "commentCount": 8500,
                                                                            "scrapId": null
                                                                        },
                                                                        "channel": {
                                                                            "id": "UC_game_channel",
                                                                            "title": "게임 채널",
                                                                            "thumbnailUrl": "https://yt3.ggpht.com/...",
                                                                            "subscriberCount": 1200000,
                                                                            "favoriteChannelId": null
                                                                        },
                                                                        "analysis": {...},
                                                                        "comments": [...]
                                                                    }
                                                                ]
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 - 지원하지 않는 카테고리 또는 잘못된 maxResults",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "잘못된 카테고리 에러",
                                            value = """
                                                    {
                                                        "httpStatus": "BAD_REQUEST",
                                                        "message": "잘못된 요청: 지원하지 않는 카테고리입니다: invalid_category",
                                                        "timeStamp": "2025-06-09T15:30:00"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "503",
                            description = "YouTube API 서비스 이용 불가",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "YouTube API 오류",
                                            value = """
                                                    {
                                                        "httpStatus": "SERVICE_UNAVAILABLE",
                                                        "message": "YouTube API에 접근할 수 없습니다",
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
                    name = "categoryType",
                    description = "인기급상승 영상 카테고리",
                    required = false,
                    in = ParameterIn.QUERY,
                    schema = @Schema(
                            type = "string",
                            allowableValues = {"latest", "music", "game"},
                            defaultValue = "latest"
                    ),
                    examples = {
                            @ExampleObject(name = "전체 카테고리", value = "latest"),
                            @ExampleObject(name = "음악", value = "music"),
                            @ExampleObject(name = "게임", value = "game")
                    }
            ),
            @Parameter(
                    name = "maxResults",
                    description = "조회할 영상 개수 (최대 30개)",
                    required = false,
                    in = ParameterIn.QUERY,
                    schema = @Schema(
                            type = "integer",
                            minimum = "1",
                            maximum = "30",
                            defaultValue = "5"
                    ),
                    example = "5"
            )
    })
    @ErrorCode400
    @ErrorCode500
    ResponseEntity<?> getByCategory(
            @CookieValue(value = "Authorization", required = false) String token,
            @RequestParam(defaultValue = "latest") String categoryType,
            @RequestParam(defaultValue = "5") int maxResults
    );
}
