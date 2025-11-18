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
                                                                    "apiChannelName": "채널명2",
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
                                                                      "positive": 72,
                                                                      "negative": 8,
                                                                      "other": 20
                                                                    },
                                                                    "keywords": ["블랙홀", "우주", "중력파", "과학"]
                                                                  }
                                                                }
                                                              }
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "로그인 사용자 - 관심 채널 없음",
                                                    summary = "관심 채널을 등록하지 않은 경우",
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
                                            ),
                                            @ExampleObject(
                                                    name = "비로그인 사용자",
                                                    summary = "토큰이 없거나 유효하지 않은 경우",
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
    @ErrorCode400
    @ErrorCode500
    ResponseEntity<ResponseDto<MainPageResponse.FavoriteChannelResponse>> getFavoriteChannels(
            @CookieValue(value = "Authorization", required = false) String token
    );

    @Operation(
            summary = "메인 페이지 - 인기 급상승 섹션",
            description = "최신 인기 급상승 영상 3개를 제공합니다.\n\n" +
                    "**제공 정보:**\n" +
                    "- 자체 알고리즘 기반 인기 영상 (검색/조회 + 스크랩 데이터)\n" +
                    "- 영상 정보, 채널 정보, AI 분석 결과 포함\n\n" +
                    "**특징:**\n" +
                    "- 인증 불필요 (모든 사용자에게 동일하게 제공)\n" +
                    "- 실시간 업데이트 반영",
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
                                                            "id": "trending_video_1",
                                                            "title": "2025 기술 트렌드 TOP 10",
                                                            "description": "올해 주목해야 할 기술 트렌드를 소개합니다",
                                                            "publishedAt": "2025-01-19T15:00:00Z",
                                                            "thumbnailUrl": "https://i.ytimg.com/vi/trending_video_1/maxresdefault.jpg",
                                                            "viewCount": 850000,
                                                            "likeCount": 42000,
                                                            "commentCount": 2145
                                                          },
                                                          "channel": {
                                                            "id": "UCxxxxTech",
                                                            "title": "테크 리뷰어",
                                                            "thumbnailUrl": "https://yt3.ggpht.com/...",
                                                            "subscriberCount": 2500000
                                                          },
                                                          "analysis": {
                                                            "summary": "AI, 양자컴퓨팅, 메타버스 등 2025년 핵심 기술 트렌드를 다룹니다. 시청자들은 정보의 깊이와 전망에 대해 긍정적으로 평가했습니다.",
                                                            "sentimentDistribution": {
                                                              "positive": 78,
                                                              "negative": 5,
                                                              "other": 17
                                                            },
                                                            "keywords": ["AI", "기술", "트렌드", "2025", "미래"]
                                                          }
                                                        },
                                                        {
                                                          "video": {
                                                            "id": "trending_video_2",
                                                            "title": "초보자를 위한 주식 투자 가이드",
                                                            "description": "주식 투자 기초부터 실전까지",
                                                            "publishedAt": "2025-01-19T12:00:00Z",
                                                            "thumbnailUrl": "https://i.ytimg.com/vi/trending_video_2/maxresdefault.jpg",
                                                            "viewCount": 620000,
                                                            "likeCount": 31000,
                                                            "commentCount": 1580
                                                          },
                                                          "channel": {
                                                            "id": "UCxxxxFinance",
                                                            "title": "재테크 전문가",
                                                            "thumbnailUrl": "https://yt3.ggpht.com/...",
                                                            "subscriberCount": 1800000
                                                          },
                                                          "analysis": {
                                                            "summary": "주식 투자의 기본 개념과 실전 노하우를 쉽게 설명합니다. 초보자들에게 유용한 내용이라는 평가가 많았습니다.",
                                                            "sentimentDistribution": {
                                                              "positive": 82,
                                                              "negative": 8,
                                                              "other": 10
                                                            },
                                                            "keywords": ["주식", "투자", "재테크", "초보자", "가이드"]
                                                          }
                                                        },
                                                        {
                                                          "video": {
                                                            "id": "trending_video_3",
                                                            "title": "겨울 제주도 여행 브이로그",
                                                            "description": "겨울 제주의 숨은 명소를 소개합니다",
                                                            "publishedAt": "2025-01-18T18:00:00Z",
                                                            "thumbnailUrl": "https://i.ytimg.com/vi/trending_video_3/maxresdefault.jpg",
                                                            "viewCount": 480000,
                                                            "likeCount": 28500,
                                                            "commentCount": 892
                                                          },
                                                          "channel": {
                                                            "id": "UCxxxxTravel",
                                                            "title": "여행 브이로거",
                                                            "thumbnailUrl": "https://yt3.ggpht.com/...",
                                                            "subscriberCount": 950000
                                                          },
                                                          "analysis": {
                                                            "summary": "겨울 제주도의 매력적인 풍경과 맛집을 소개합니다. 영상미와 정보성 모두 좋다는 반응입니다.",
                                                            "sentimentDistribution": {
                                                              "positive": 88,
                                                              "negative": 3,
                                                              "other": 9
                                                            },
                                                            "keywords": ["제주도", "여행", "겨울", "브이로그", "맛집"]
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
    @ErrorCode400
    @ErrorCode500
    ResponseEntity<ResponseDto<List<VideoSummaryResponse>>> getTrending(
            @CookieValue(value = "Authorization", required = false) String token
    );

    @Operation(
            summary = "메인 페이지 - 스크랩 섹션",
            description = "사용자가 스크랩한 영상 목록을 제공합니다.\n\n" +
                    "**제공 정보:**\n" +
                    "- 최대 3개의 스크랩 영상 (최신순)\n" +
                    "- 영상 정보, 채널 정보, AI 분석 결과 포함\n" +
                    "- 삭제된 영상 표시 지원\n\n" +
                    "**특징:**\n" +
                    "- 인증 필수\n" +
                    "- 스크랩 없으면 빈 배열 반환",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "스크랩 섹션 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseDto.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "스크랩 영상 목록",
                                                    summary = "스크랩한 영상이 있는 경우",
                                                    value = """
                                                            {
                                                              "timeStamp": "2025-01-20T10:00:00",
                                                              "message": "스크랩 섹션 조회 완료",
                                                              "data": [
                                                                {
                                                                  "video": {
                                                                    "id": "scrap_video_1",
                                                                    "title": "프로그래밍 기초 강의 #1",
                                                                    "description": "파이썬 기초부터 시작하기",
                                                                    "publishedAt": "2025-01-15T10:00:00Z",
                                                                    "thumbnailUrl": "https://i.ytimg.com/vi/scrap_video_1/maxresdefault.jpg",
                                                                    "viewCount": 52000,
                                                                    "likeCount": 2800,
                                                                    "commentCount": 185
                                                                  },
                                                                  "channel": {
                                                                    "id": "UCxxxxCoding",
                                                                    "title": "코딩 강사",
                                                                    "thumbnailUrl": "https://yt3.ggpht.com/...",
                                                                    "subscriberCount": 450000
                                                                  },
                                                                  "analysis": {
                                                                    "summary": "파이썬 프로그래밍의 기초를 다루는 입문 강의입니다. 초보자도 따라하기 쉽다는 평가가 많습니다.",
                                                                    "sentimentDistribution": {
                                                                      "positive": 85,
                                                                      "negative": 5,
                                                                      "other": 10
                                                                    },
                                                                    "keywords": ["파이썬", "프로그래밍", "코딩", "기초", "강의"]
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
                                                                      "positive": 91,
                                                                      "negative": 2,
                                                                      "other": 7
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