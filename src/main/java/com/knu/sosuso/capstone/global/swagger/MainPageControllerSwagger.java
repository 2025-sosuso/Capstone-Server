package com.knu.sosuso.capstone.global.swagger;

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
                                                    summary = "관심 채널과 최신 영상 정보",
                                                    value = """
                                                            {
                                                              "timeStamp": "2025-01-20T10:00:00",
                                                              "message": "관심 채널 섹션 조회 완료",
                                                              "data": {
                                                                "favoriteChannelList": [
                                                                  {
                                                                    "favoriteChannelId": 1,
                                                                    "apiChannelId": "UCZf__ehlCEBPop-_sldpBUQ",
                                                                    "apiChannelName": "EBS 다큐",
                                                                    "apiChannelThumbnail": "https://yt3.ggpht.com/..."
                                                                  },
                                                                  {
                                                                    "favoriteChannelId": 2,
                                                                    "apiChannelId": "UC-lHJZR3Gqxm24_Vd_AJ5Yw",
                                                                    "apiChannelName": "PewDiePie",
                                                                    "apiChannelThumbnail": "https://yt3.ggpht.com/..."
                                                                  }
                                                                ],
                                                                "videoSummary": {
                                                                  "video": {
                                                                    "id": "dQw4w9WgXcQ",
                                                                    "title": "[다큐] 우주의 신비 - 블랙홀의 비밀",
                                                                    "description": "블랙홀에 대한 최신 연구 결과를 소개합니다...",
                                                                    "publishedAt": "2025-01-20T09:00:00Z",
                                                                    "thumbnailUrl": "https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
                                                                    "viewCount": 125000,
                                                                    "likeCount": 8500,
                                                                    "commentCount": 342
                                                                  },
                                                                  "channel": {
                                                                    "id": "UCZf__ehlCEBPop-_sldpBUQ",
                                                                    "title": "EBS 다큐",
                                                                    "thumbnailUrl": "https://yt3.ggpht.com/...",
                                                                    "subscriberCount": 1250000
                                                                  },
                                                                  "analysis": {
                                                                    "summary": "블랙홀의 형성 과정과 중력파 관측 성과를 다룬 영상입니다. 시청자들은 과학적 설명이 명확하고 시각 자료가 이해하기 쉽다는 반응을 보였습니다.",
                                                                    "sentimentDistribution": {
                                                                      "positive": 0.72,
                                                                      "negative": 0.08,
                                                                      "other": 0.20
                                                                    },
                                                                    "keywords": ["블랙홀", "우주", "중력파", "과학"]
                                                                  }
                                                                }
                                                              }
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "비로그인 사용자 또는 관심 채널 없음",
                                                    summary = "빈 데이터 반환",
                                                    value = """
                                                            {
                                                              "timeStamp": "2025-01-20T10:00:00",
                                                              "message": "관심 채널 섹션 조회 완료",
                                                              "data": {
                                                                "favoriteChannelList": [],
                                                                "videoSummary": null
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
                                            summary = "최대 3개의 인기 영상",
                                            value = """
                                                    {
                                                      "timeStamp": "2025-01-20T10:00:00",
                                                      "message": "인기 급상승 섹션 조회 완료",
                                                      "data": [
                                                        {
                                                          "video": {
                                                            "id": "abc123def456",
                                                            "title": "[MV] 뉴진스(NewJeans) - OMG",
                                                            "description": "NewJeans 'OMG' Official MV...",
                                                            "publishedAt": "2025-01-19T15:00:00Z",
                                                            "thumbnailUrl": "https://i.ytimg.com/vi/abc123def456/maxresdefault.jpg",
                                                            "viewCount": 5234567,
                                                            "likeCount": 423000,
                                                            "commentCount": 12450
                                                          },
                                                          "channel": {
                                                            "id": "UCOmHUn--16B90oW2L6FRR3A",
                                                            "title": "HYBE LABELS",
                                                            "thumbnailUrl": "https://yt3.ggpht.com/...",
                                                            "subscriberCount": 68500000
                                                          },
                                                          "analysis": {
                                                            "summary": "NewJeans의 신곡 OMG에 대한 반응이 뜨겁습니다. 중독성 있는 멜로디와 독특한 컨셉에 대한 호평이 이어지고 있습니다.",
                                                            "sentimentDistribution": {
                                                              "positive": 0.85,
                                                              "negative": 0.05,
                                                              "other": 0.10
                                                            },
                                                            "keywords": ["뉴진스", "OMG", "케이팝", "중독성", "신곡"]
                                                          }
                                                        },
                                                        {
                                                          "video": {
                                                            "id": "xyz789ghi012",
                                                            "title": "이번주 LOL 챔피언스 하이라이트",
                                                            "description": "2025 LCK Spring 1주차 최고의 순간들",
                                                            "publishedAt": "2025-01-19T20:30:00Z",
                                                            "thumbnailUrl": "https://i.ytimg.com/vi/xyz789ghi012/maxresdefault.jpg",
                                                            "viewCount": 892000,
                                                            "likeCount": 45200,
                                                            "commentCount": 3890
                                                          },
                                                          "channel": {
                                                            "id": "UCXePl3KM0Ix_dbNikDATbAg",
                                                            "title": "LCK Korea",
                                                            "thumbnailUrl": "https://yt3.ggpht.com/...",
                                                            "subscriberCount": 3250000
                                                          },
                                                          "analysis": {
                                                            "summary": "이번 주 LCK 경기의 명장면을 모은 하이라이트입니다. 특히 T1의 화려한 플레이가 화제입니다.",
                                                            "sentimentDistribution": {
                                                              "positive": 0.78,
                                                              "negative": 0.12,
                                                              "other": 0.10
                                                            },
                                                            "keywords": ["LCK", "롤", "T1", "하이라이트"]
                                                          }
                                                        },
                                                        {
                                                          "video": {
                                                            "id": "qwe456rty789",
                                                            "title": "[리뷰] 갤럭시 S25 울트라 개봉기",
                                                            "description": "삼성 갤럭시 S25 울트라 실물 리뷰",
                                                            "publishedAt": "2025-01-19T12:00:00Z",
                                                            "thumbnailUrl": "https://i.ytimg.com/vi/qwe456rty789/maxresdefault.jpg",
                                                            "viewCount": 645000,
                                                            "likeCount": 28900,
                                                            "commentCount": 2340
                                                          },
                                                          "channel": {
                                                            "id": "UCOfOHs0vE8JKdV1uLVq42XQ",
                                                            "title": "잇섭 itSub",
                                                            "thumbnailUrl": "https://yt3.ggpht.com/...",
                                                            "subscriberCount": 1850000
                                                          },
                                                          "analysis": {
                                                            "summary": "갤럭시 S25 울트라의 디자인과 성능에 대한 첫 인상 리뷰입니다. 카메라 성능 향상이 주목받고 있습니다.",
                                                            "sentimentDistribution": {
                                                              "positive": 0.68,
                                                              "negative": 0.18,
                                                              "other": 0.14
                                                            },
                                                            "keywords": ["갤럭시", "S25", "스마트폰", "리뷰", "개봉"]
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
                    "- 스크랩한 영상 정보 (최신순)\n" +
                    "- 채널 정보\n" +
                    "- AI 분석 결과\n" +
                    "- 삭제된 영상 포함 (표시만 다르게)\n\n" +
                    "**인증:** 필수 (토큰 없으면 401 에러)",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "스크랩 섹션 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseDto.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "스크랩 영상 목록 (정상)",
                                                    summary = "정상적인 스크랩 영상들",
                                                    value = """
                                                            {
                                                              "timeStamp": "2025-01-20T10:00:00",
                                                              "message": "스크랩 섹션 조회 완료",
                                                              "data": [
                                                                {
                                                                  "video": {
                                                                    "id": "scrap_video_1",
                                                                    "title": "파이썬 코딩 기초 강좌 #1",
                                                                    "description": "파이썬 기초부터 차근차근...",
                                                                    "publishedAt": "2025-01-15T10:00:00Z",
                                                                    "thumbnailUrl": "https://i.ytimg.com/vi/scrap_video_1/maxresdefault.jpg",
                                                                    "viewCount": 89000,
                                                                    "likeCount": 3200,
                                                                    "commentCount": 450
                                                                  },
                                                                  "channel": {
                                                                    "id": "UCxxxxPython",
                                                                    "title": "코딩애플",
                                                                    "thumbnailUrl": "https://yt3.ggpht.com/...",
                                                                    "subscriberCount": 450000
                                                                  },
                                                                  "analysis": {
                                                                    "summary": "파이썬 입문자를 위한 친절한 강의입니다. 실습 위주로 구성되어 초보자도 쉽게 따라할 수 있습니다.",
                                                                    "sentimentDistribution": {
                                                                      "positive": 0.82,
                                                                      "negative": 0.05,
                                                                      "other": 0.13
                                                                    },
                                                                    "keywords": ["파이썬", "코딩", "프로그래밍", "강의"]
                                                                  }
                                                                },
                                                                {
                                                                  "video": {
                                                                    "id": "scrap_video_2",
                                                                    "title": "요리 브이로그 | 김치찌개 끓이기",
                                                                    "description": "집에서 쉽게 만드는 김치찌개",
                                                                    "publishedAt": "2025-01-18T14:30:00Z",
                                                                    "thumbnailUrl": "https://i.ytimg.com/vi/scrap_video_2/maxresdefault.jpg",
                                                                    "viewCount": 12400,
                                                                    "likeCount": 890,
                                                                    "commentCount": 67
                                                                  },
                                                                  "channel": {
                                                                    "id": "UCxxxxCooking",
                                                                    "title": "집밥하는 유튜버",
                                                                    "thumbnailUrl": "https://yt3.ggpht.com/...",
                                                                    "subscriberCount": 85000
                                                                  },
                                                                  "analysis": {
                                                                    "summary": "간단하고 맛있는 김치찌개 레시피입니다. 재료 손질부터 완성까지 상세히 설명합니다.",
                                                                    "sentimentDistribution": {
                                                                      "positive": 0.91,
                                                                      "negative": 0.02,
                                                                      "other": 0.07
                                                                    },
                                                                    "keywords": ["요리", "김치찌개", "레시피", "집밥"]
                                                                  }
                                                                }
                                                              ]
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "스크랩 없음",
                                                    summary = "스크랩한 영상이 없는 경우",
                                                    value = """
                                                            {
                                                              "timeStamp": "2025-01-20T10:00:00",
                                                              "message": "스크랩 섹션 조회 완료",
                                                              "data": []
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "삭제된 영상 포함",
                                                    summary = "삭제된 영상이 포함된 경우",
                                                    value = """
                                                            {
                                                              "timeStamp": "2025-01-20T10:00:00",
                                                              "message": "스크랩 섹션 조회 완료",
                                                              "data": [
                                                                {
                                                                  "video": {
                                                                    "id": "deleted_video",
                                                                    "title": "[삭제된 영상] 이전 영상 제목",
                                                                    "description": "이 영상은 삭제되었거나 비공개 처리되었습니다.",
                                                                    "publishedAt": "2025-01-10T10:00:00Z",
                                                                    "thumbnailUrl": "https://i.ytimg.com/vi/deleted_video/default.jpg",
                                                                    "viewCount": 0,
                                                                    "likeCount": 0,
                                                                    "commentCount": 0
                                                                  },
                                                                  "channel": {
                                                                    "id": "UCxxxxDeleted",
                                                                    "title": "이전 채널명",
                                                                    "thumbnailUrl": "https://yt3.ggpht.com/...",
                                                                    "subscriberCount": 0
                                                                  },
                                                                  "analysis": {
                                                                    "summary": "이 영상은 삭제되었습니다.",
                                                                    "sentimentDistribution": null,
                                                                    "keywords": []
                                                                  }
                                                                }
                                                              ]
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패 - 토큰 누락 또는 유효하지 않음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "인증 에러",
                                            value = """
                                                    {
                                                      "httpStatus": "UNAUTHORIZED",
                                                      "message": "유효하지 않은 토큰입니다.",
                                                      "timeStamp": "2025-01-20T10:00:00"
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