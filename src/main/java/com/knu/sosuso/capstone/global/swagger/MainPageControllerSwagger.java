package com.knu.sosuso.capstone.global.swagger;

import com.knu.sosuso.capstone.domain.channel.dto.response.FavoriteChannelListResponse;
import com.knu.sosuso.capstone.domain.main.MainPageResponse;
import com.knu.sosuso.capstone.domain.video.dto.response.VideoSummaryResponse;
import com.knu.sosuso.capstone.global.ResponseDto;
import com.knu.sosuso.capstone.global.exception.ErrorResponse;
import com.knu.sosuso.capstone.global.swagger.annotation.ErrorCode400;
import com.knu.sosuso.capstone.global.swagger.annotation.ErrorCode500;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;

import java.util.List;

@Tag(
        name = "메인 페이지 API",
        description = "메인 페이지의 섹션별 데이터를 제공하는 API입니다. " +
                "관심 채널, 인기 급상승, 스크랩 섹션을 독립적으로 조회할 수 있습니다."
)
public interface MainPageControllerSwagger {

    @Operation(
            summary = "메인 페이지 - 관심 채널 섹션",
            description = "사용자의 관심 채널 목록과 첫 번째 채널의 최신 영상 정보를 제공합니다.\n\n" +
                    "**제공 정보:**\n" +
                    "- 관심 채널 목록\n" +
                    "- 첫 번째 채널의 최신 업로드 영상 1개 (영상 정보, 채널 정보, AI 분석 포함)\n\n" +
                    "**인증:**\n" +
                    "- 토큰 없음/유효하지 않음 → 빈 데이터 반환\n" +
                    "- 유효한 토큰 → 사용자의 관심 채널 데이터 반환",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "관심 채널 섹션 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseDto.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "로그인 사용자 - 관심 채널 있음",
                                                    value = """
                                                            {
                                                              "timeStamp": "2025-01-20T10:00:00",
                                                              "message": "관심 채널 섹션 조회 완료",
                                                              "data": {
                                                                "favoriteChannelList": [
                                                                  {
                                                                    "favoriteChannelId": 1,
                                                                    "apiChannelId": "UCxxxxxxxx",
                                                                    "apiChannelName": "채널명",
                                                                    "apiChannelThumbnail": "https://..."
                                                                  }
                                                                ],
                                                                "latestVideo": {
                                                                  "video": {
                                                                    "id": "videoId123",
                                                                    "title": "최신 영상 제목",
                                                                    "description": "영상 설명...",
                                                                    "publishedAt": "2025-01-20T00:00:00Z",
                                                                    "thumbnailUrl": "https://...",
                                                                    "viewCount": 10000,
                                                                    "likeCount": 500,
                                                                    "commentCount": 100
                                                                  },
                                                                  "channel": {
                                                                    "id": "UCxxxxxxxx",
                                                                    "title": "채널명",
                                                                    "thumbnailUrl": "https://...",
                                                                    "subscriberCount": 100000
                                                                  },
                                                                  "analysis": {
                                                                    "summary": "AI 요약 내용...",
                                                                    "sentimentDistribution": {
                                                                      "positive": 0.6,
                                                                      "negative": 0.2,
                                                                      "other": 0.2
                                                                    },
                                                                    "keywords": ["키워드1", "키워드2"],
                                                                    "topComments": [...]
                                                                  }
                                                                }
                                                              }
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "비로그인 사용자 또는 관심 채널 없음",
                                                    value = """
                                                            {
                                                              "timeStamp": "2025-01-20T10:00:00",
                                                              "message": "관심 채널 섹션 조회 완료",
                                                              "data": {
                                                                "favoriteChannelList": [],
                                                                "latestVideo": null
                                                              }
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    @Parameter(
            name = "Authorization",
            description = "JWT 토큰 (Cookie) - 선택사항",
            required = false,
            in = ParameterIn.COOKIE,
            schema = @Schema(type = "string", format = "jwt")
    )
    @ErrorCode500
    ResponseEntity<ResponseDto<MainPageResponse.FavoriteChannelResponse>> getFavoriteChannels(
            @CookieValue(value = "Authorization", required = false) String token
    );

    @Operation(
            summary = "메인 페이지 - 인기 급상승 섹션",
            description = "한국(KR) 기준 최신 인기 급상승 영상 3개를 제공합니다.\n\n" +
                    "**제공 정보:**\n" +
                    "- 영상 기본 정보\n" +
                    "- 채널 정보\n" +
                    "- AI 분석 결과 (요약, 감정, 키워드 등)\n\n" +
                    "**특징:**\n" +
                    "- 인증 불필요 (모든 사용자 동일 데이터)\n" +
                    "- 로그인 시 스크랩/관심채널 ID 추가 제공",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "인기 급상승 섹션 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseDto.class),
                                    examples = @ExampleObject(
                                            name = "인기 급상승 영상 목록",
                                            value = """
                                                    {
                                                      "timeStamp": "2025-01-20T10:00:00",
                                                      "message": "인기 급상승 섹션 조회 완료",
                                                      "data": [
                                                        {
                                                          "video": {
                                                            "id": "trending1",
                                                            "title": "인기 영상 1",
                                                            "description": "설명...",
                                                            "publishedAt": "2025-01-19T00:00:00Z",
                                                            "thumbnailUrl": "https://...",
                                                            "viewCount": 500000,
                                                            "likeCount": 20000,
                                                            "commentCount": 3000
                                                          },
                                                          "channel": {
                                                            "id": "UCyyyyyyyy",
                                                            "title": "인기 채널",
                                                            "thumbnailUrl": "https://...",
                                                            "subscriberCount": 1000000
                                                          },
                                                          "analysis": {
                                                            "summary": "AI 요약...",
                                                            "sentimentDistribution": {
                                                              "positive": 0.7,
                                                              "negative": 0.1,
                                                              "other": 0.2
                                                            },
                                                            "keywords": ["키워드1", "키워드2"]
                                                          }
                                                        }
                                                      ]
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    @Parameter(
            name = "Authorization",
            description = "JWT 토큰 (Cookie) - 선택사항",
            required = false,
            in = ParameterIn.COOKIE,
            schema = @Schema(type = "string", format = "jwt")
    )
    @ErrorCode500
    ResponseEntity<ResponseDto<List<VideoSummaryResponse>>> getTrending(
            @CookieValue(value = "Authorization", required = false) String token
    );

    @Operation(
            summary = "메인 페이지 - 스크랩 섹션",
            description = "사용자가 스크랩한 영상 목록을 최대 3개까지 제공합니다.\n\n" +
                    "**제공 정보:**\n" +
                    "- 스크랩한 영상 정보\n" +
                    "- 채널 정보\n" +
                    "- AI 분석 결과\n\n" +
                    "**인증:** 필수 (토큰 없으면 400 에러)",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "스크랩 섹션 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseDto.class),
                                    examples = @ExampleObject(
                                            name = "스크랩 영상 목록",
                                            value = """
                                                    {
                                                      "timeStamp": "2025-01-20T10:00:00",
                                                      "message": "스크랩 섹션 조회 완료",
                                                      "data": [
                                                        {
                                                          "video": {
                                                            "id": "scrap1",
                                                            "title": "스크랩한 영상",
                                                            "description": "설명...",
                                                            "publishedAt": "2025-01-18T00:00:00Z",
                                                            "thumbnailUrl": "https://...",
                                                            "viewCount": 50000,
                                                            "likeCount": 2000,
                                                            "commentCount": 300
                                                          },
                                                          "channel": {...},
                                                          "analysis": {...}
                                                        }
                                                      ]
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "토큰 누락 또는 유효하지 않음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            },
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @Parameter(
            name = "Authorization",
            description = "JWT 토큰 (Cookie) - 필수",
            required = true,
            in = ParameterIn.COOKIE,
            schema = @Schema(type = "string", format = "jwt")
    )
    @ErrorCode400
    @ErrorCode500
    ResponseEntity<ResponseDto<List<VideoSummaryResponse>>> getScraps(
            @CookieValue(value = "Authorization") String token
    );
}