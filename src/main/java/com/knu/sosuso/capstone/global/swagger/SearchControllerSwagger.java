package com.knu.sosuso.capstone.global.swagger;

import com.knu.sosuso.capstone.global.ResponseDto;
import com.knu.sosuso.capstone.domain.video.dto.response.SearchResultPageResponse;
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
        name = "검색 API",
        description = "YouTube 영상, 쇼츠, 채널 검색 API입니다. Fast Path 아키텍처를 사용하여 캐시된 데이터를 즉시 반환하고, 백그라운드에서 AI 분석을 수행합니다."
)
public interface SearchControllerSwagger {

    @Operation(
            summary = "동영상 검색 (쇼츠 제외)",
            description = "YouTube 동영상을 검색합니다. (쇼츠는 제외됩니다)\n\n" +
                    "**Fast Path 아키텍처:**\n" +
                    "- 캐시된 데이터를 즉시 반환 (0.1초 이내)\n" +
                    "- 백그라운드에서 AI 분석 비동기 처리\n" +
                    "- AI 분석이 완료되지 않은 경우 기본값(null) 반환\n\n" +
                    "**무한 스크롤 지원:**\n" +
                    "- `nextPageToken`을 사용하여 다음 페이지 조회\n" +
                    "- `hasMore`로 추가 데이터 존재 여부 확인\n\n" +
                    "**응답 데이터:**\n" +
                    "- 영상 기본 정보 (제목, 조회수, 좋아요 등)\n" +
                    "- 채널 정보 (이름, 구독자 수 등)\n" +
                    "- AI 분석 결과 (요약, 감정 분포, 키워드) - 분석 완료 시에만\n" +
                    "- 스크랩 여부 (로그인 사용자만)",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "검색 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseDto.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "첫 페이지 - AI 분석 완료",
                                                    summary = "첫 페이지 조회, AI 분석이 완료된 영상",
                                                    value = """
                                                            {
                                                              "timeStamp": "2025-11-07T18:30:00",
                                                              "message": "동영상 검색 완료",
                                                              "data": {
                                                                "results": [
                                                                  {
                                                                    "video": {
                                                                      "id": "dQw4w9WgXcQ",
                                                                      "title": "Never Gonna Give You Up",
                                                                      "description": "Rick Astley's official music video...",
                                                                      "publishedAt": "2009-10-25T06:57:33Z",
                                                                      "thumbnailUrl": "https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
                                                                      "viewCount": 1500000000,
                                                                      "likeCount": 15000000,
                                                                      "commentCount": 4500000
                                                                    },
                                                                    "channel": {
                                                                      "id": "UCuAXFkgsw1L7xaCfnd5JJOw",
                                                                      "title": "Rick Astley",
                                                                      "thumbnailUrl": "https://yt3.ggpht.com/...",
                                                                      "subscriberCount": 5000000
                                                                    },
                                                                    "analysis": {
                                                                      "summary": "이 영상은 Rick Astley의 대표곡으로...",
                                                                      "sentimentDistribution": {
                                                                        "positive": 85,
                                                                        "negative": 5,
                                                                        "other": 10
                                                                      },
                                                                      "keywords": ["음악", "클래식", "80년대"]
                                                                    }
                                                                  }
                                                                ],
                                                                "nextPageToken": "CAoQAA",
                                                                "totalResults": 1,
                                                                "hasMore": true
                                                              }
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "첫 페이지 - AI 분석 미완료",
                                                    summary = "첫 페이지 조회, AI 분석이 아직 완료되지 않은 영상",
                                                    value = """
                                                            {
                                                              "timeStamp": "2025-11-07T18:30:00",
                                                              "message": "동영상 검색 완료",
                                                              "data": {
                                                                "results": [
                                                                  {
                                                                    "video": {
                                                                      "id": "xyz123",
                                                                      "title": "최신 업로드 영상",
                                                                      "description": "방금 업로드된 영상...",
                                                                      "publishedAt": "2025-11-07T18:00:00Z",
                                                                      "thumbnailUrl": "https://i.ytimg.com/vi/xyz123/maxresdefault.jpg",
                                                                      "viewCount": 1000,
                                                                      "likeCount": 50,
                                                                      "commentCount": 10
                                                                    },
                                                                    "channel": {
                                                                      "id": "UCabc123",
                                                                      "title": "새로운 채널",
                                                                      "thumbnailUrl": "https://yt3.ggpht.com/...",
                                                                      "subscriberCount": 10000
                                                                    },
                                                                    "analysis": null
                                                                  }
                                                                ],
                                                                "nextPageToken": "CAoQAA",
                                                                "totalResults": 1,
                                                                "hasMore": true
                                                              }
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "다음 페이지",
                                                    summary = "nextPageToken을 사용한 페이지네이션",
                                                    value = """
                                                            {
                                                              "timeStamp": "2025-11-07T18:30:00",
                                                              "message": "동영상 검색 완료",
                                                              "data": {
                                                                "results": [
                                                                  {
                                                                    "video": {
                                                                      "id": "abc456",
                                                                      "title": "두 번째 페이지 영상",
                                                                      "description": "검색 결과 중...",
                                                                      "publishedAt": "2025-11-06T12:00:00Z",
                                                                      "thumbnailUrl": "https://i.ytimg.com/vi/abc456/maxresdefault.jpg",
                                                                      "viewCount": 500000,
                                                                      "likeCount": 25000,
                                                                      "commentCount": 1200
                                                                    },
                                                                    "channel": {
                                                                      "id": "UCdef789",
                                                                      "title": "인기 채널",
                                                                      "thumbnailUrl": "https://yt3.ggpht.com/...",
                                                                      "subscriberCount": 1000000
                                                                    },
                                                                    "analysis": {
                                                                      "summary": "이 영상은...",
                                                                      "sentimentDistribution": {
                                                                        "positive": 70,
                                                                        "negative": 15,
                                                                        "other": 15
                                                                      },
                                                                      "keywords": ["리뷰", "제품", "추천"]
                                                                    }
                                                                  }
                                                                ],
                                                                "nextPageToken": "CAoQFA",
                                                                "totalResults": 2,
                                                                "hasMore": false
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
                                                        "message": "검색어는 필수입니다.",
                                                        "timeStamp": "2025-11-07T18:30:00"
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
                                                        "message": "검색 중 오류가 발생했습니다.",
                                                        "timeStamp": "2025-11-07T18:30:00"
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
                    description = "JWT 토큰 (선택사항) - 로그인 시 스크랩 정보 포함",
                    required = false,
                    in = ParameterIn.COOKIE,
                    schema = @Schema(type = "string")
            ),
            @Parameter(
                    name = "query",
                    description = "검색어",
                    required = true,
                    in = ParameterIn.QUERY,
                    schema = @Schema(type = "string"),
                    example = "프로그래밍 강의"
            ),
            @Parameter(
                    name = "pageToken",
                    description = "다음 페이지 토큰 (첫 요청 시 생략, 이후 응답의 nextPageToken 사용)",
                    required = false,
                    in = ParameterIn.QUERY,
                    schema = @Schema(type = "string"),
                    example = "CAoQAA"
            )
    })
    @ErrorCode400
    @ErrorCode500
    ResponseEntity<ResponseDto<SearchResultPageResponse>> searchVideos(
            @CookieValue(value = "Authorization", required = false) String token,
            @RequestParam String query,
            @RequestParam(required = false) String pageToken
    );

    @Operation(
            summary = "쇼츠 검색",
            description = "YouTube 쇼츠를 검색합니다.\n\n" +
                    "**Fast Path 아키텍처:**\n" +
                    "- 캐시된 데이터를 즉시 반환 (0.1초 이내)\n" +
                    "- 백그라운드에서 AI 분석 비동기 처리\n" +
                    "- AI 분석이 완료되지 않은 경우 기본값(null) 반환\n\n" +
                    "**무한 스크롤 지원:**\n" +
                    "- `nextPageToken`을 사용하여 다음 페이지 조회\n" +
                    "- `hasMore`로 추가 데이터 존재 여부 확인\n\n" +
                    "**쇼츠 특징:**\n" +
                    "- 세로형 영상 (9:16 비율)\n" +
                    "- 60초 이하 길이\n" +
                    "- 모바일 최적화",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "검색 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseDto.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "쇼츠 검색 결과",
                                                    summary = "쇼츠 검색 성공",
                                                    value = """
                                                            {
                                                              "timeStamp": "2025-11-07T18:30:00",
                                                              "message": "쇼츠 검색 완료",
                                                              "data": {
                                                                "results": [
                                                                  {
                                                                    "video": {
                                                                      "id": "shorts123",
                                                                      "title": "1분 코딩 팁",
                                                                      "description": "간단한 파이썬 팁...",
                                                                      "publishedAt": "2025-11-07T10:00:00Z",
                                                                      "thumbnailUrl": "https://i.ytimg.com/vi/shorts123/maxresdefault.jpg",
                                                                      "viewCount": 100000,
                                                                      "likeCount": 5000,
                                                                      "commentCount": 200
                                                                    },
                                                                    "channel": {
                                                                      "id": "UCshorts",
                                                                      "title": "코딩 쇼츠",
                                                                      "thumbnailUrl": "https://yt3.ggpht.com/...",
                                                                      "subscriberCount": 50000
                                                                    },
                                                                    "analysis": {
                                                                      "summary": "파이썬 기초 팁을 설명하는...",
                                                                      "sentimentDistribution": {
                                                                        "positive": 90,
                                                                        "negative": 3,
                                                                        "other": 7
                                                                      },
                                                                      "keywords": ["파이썬", "코딩", "팁"]
                                                                    }
                                                                  }
                                                                ],
                                                                "nextPageToken": "CAoQAA",
                                                                "totalResults": 1,
                                                                "hasMore": true
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
                                                        "message": "검색어는 필수입니다.",
                                                        "timeStamp": "2025-11-07T18:30:00"
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
                                                        "message": "검색 중 오류가 발생했습니다.",
                                                        "timeStamp": "2025-11-07T18:30:00"
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
                    description = "JWT 토큰 (선택사항) - 로그인 시 스크랩 정보 포함",
                    required = false,
                    in = ParameterIn.COOKIE,
                    schema = @Schema(type = "string")
            ),
            @Parameter(
                    name = "query",
                    description = "검색어",
                    required = true,
                    in = ParameterIn.QUERY,
                    schema = @Schema(type = "string"),
                    example = "댄스"
            ),
            @Parameter(
                    name = "pageToken",
                    description = "다음 페이지 토큰 (첫 요청 시 생략, 이후 응답의 nextPageToken 사용)",
                    required = false,
                    in = ParameterIn.QUERY,
                    schema = @Schema(type = "string"),
                    example = "CAoQAA"
            )
    })
    @ErrorCode400
    @ErrorCode500
    ResponseEntity<ResponseDto<SearchResultPageResponse>> searchShorts(
            @CookieValue(value = "Authorization", required = false) String token,
            @RequestParam String query,
            @RequestParam(required = false) String pageToken
    );

    @Operation(
            summary = "채널 검색",
            description = "YouTube 채널을 검색합니다.\n\n" +
                    "**검색 기능:**\n" +
                    "- 채널명으로 검색\n" +
                    "- 구독자 수 기준 정렬 (내림차순)\n" +
                    "- 로그인 사용자: 관심 채널 정보 포함\n\n" +
                    "**캐싱:**\n" +
                    "- YouTube API 호출 절감을 위해 1시간 캐싱\n" +
                    "- 사용자별로 다른 결과 (관심 채널 정보)\n\n" +
                    "**검색 로그:**\n" +
                    "- 모든 검색어는 자동으로 로그에 저장\n" +
                    "- 인기 검색어 분석에 활용",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "검색 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseDto.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "채널 검색 결과 (비로그인)",
                                                    summary = "비로그인 사용자의 채널 검색",
                                                    value = """
                                                            {
                                                                "timeStamp": "2025-11-07T18:30:00",
                                                                "message": "채널 검색 완료",
                                                                "data": [
                                                                    {
                                                                        "id": "UCmGSJVG3mCRXVOP4yZrU1Dw",
                                                                        "title": "테크 리뷰 채널",
                                                                        "handle": "@techreview",
                                                                        "description": "최신 기술 제품을 리뷰합니다...",
                                                                        "thumbnailUrl": "https://yt3.ggpht.com/...",
                                                                        "subscriberCount": 1500000,
                                                                        "favoriteChannelId": null
                                                                    },
                                                                    {
                                                                        "id": "UCabc123xyz",
                                                                        "title": "코딩 튜토리얼",
                                                                        "handle": "@codingtutorial",
                                                                        "description": "프로그래밍 강의를 제공합니다...",
                                                                        "thumbnailUrl": "https://yt3.ggpht.com/...",
                                                                        "subscriberCount": 800000,
                                                                        "favoriteChannelId": null
                                                                    }
                                                                ]
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "채널 검색 결과 (로그인)",
                                                    summary = "로그인 사용자의 채널 검색 - 관심 채널 정보 포함",
                                                    value = """
                                                            {
                                                                "timeStamp": "2025-11-07T18:30:00",
                                                                "message": "채널 검색 완료",
                                                                "data": [
                                                                    {
                                                                        "id": "UCmGSJVG3mCRXVOP4yZrU1Dw",
                                                                        "title": "테크 리뷰 채널",
                                                                        "handle": "@techreview",
                                                                        "description": "최신 기술 제품을 리뷰합니다...",
                                                                        "thumbnailUrl": "https://yt3.ggpht.com/...",
                                                                        "subscriberCount": 1500000,
                                                                        "favoriteChannelId": 42
                                                                    },
                                                                    {
                                                                        "id": "UCabc123xyz",
                                                                        "title": "코딩 튜토리얼",
                                                                        "handle": "@codingtutorial",
                                                                        "description": "프로그래밍 강의를 제공합니다...",
                                                                        "thumbnailUrl": "https://yt3.ggpht.com/...",
                                                                        "subscriberCount": 800000,
                                                                        "favoriteChannelId": null
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
                            description = "잘못된 요청",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                        "httpStatus": "BAD_REQUEST",
                                                        "message": "검색어는 필수입니다.",
                                                        "timeStamp": "2025-11-07T18:30:00"
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
                                                        "message": "검색 중 오류가 발생했습니다.",
                                                        "timeStamp": "2025-11-07T18:30:00"
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
                    description = "채널 검색어",
                    required = true,
                    in = ParameterIn.QUERY,
                    schema = @Schema(type = "string"),
                    example = "테크 리뷰"
            )
    })
    @ErrorCode400
    @ErrorCode500
    ResponseEntity<ResponseDto<?>> searchChannels(
            @CookieValue(value = "Authorization", required = false) String token,
            @RequestParam String query
    );
}