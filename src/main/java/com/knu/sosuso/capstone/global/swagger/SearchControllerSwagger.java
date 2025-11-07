package com.knu.sosuso.capstone.global.swagger;

import com.knu.sosuso.capstone.global.ResponseDto;
import com.knu.sosuso.capstone.domain.video.dto.response.SearchApiResponse;
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

    @Deprecated
    @Operation(
            summary = "[Deprecated] 통합 검색",
            description = "⚠️ **이 API는 Deprecated 되었습니다.** 새로운 분리된 API를 사용해주세요:\n" +
                    "- 동영상: `/api/search/videos`\n" +
                    "- 쇼츠: `/api/search/shorts`\n" +
                    "- 채널: `/api/search/channels`\n\n" +
                    "YouTube URL 또는 검색어를 통한 통합 검색 기능입니다.\n\n" +
                    "**검색 타입:**\n" +
                    "- **YouTube URL 입력**: apiVideoId 반환 → 프론트엔드가 상세 페이지(/videos/{apiVideoId})로 이동하여 분리된 API 호출\n" +
                    "- **일반 검색어 입력**: 채널 검색 결과 반환\n\n" +
                    "**지원하는 YouTube URL 형식:**\n" +
                    "- `https://www.youtube.com/watch?v=VIDEO_ID`\n" +
                    "- `https://youtu.be/VIDEO_ID`",
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
                                                              "timeStamp": "2025-11-07T18:30:00",
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
                                                    name = "채널 검색",
                                                    summary = "채널 검색 결과",
                                                    value = """
                                                            {
                                                                "timeStamp": "2025-11-07T18:30:00",
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
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "서버 오류",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    @Parameters({
            @Parameter(
                    name = "Authorization",
                    description = "JWT 토큰 (선택사항)",
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
                                                                        "positive": 0.85,
                                                                        "negative": 0.05,
                                                                        "other": 0.10
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
                                                    name = "첫 페이지 - AI 분석 진행중",
                                                    summary = "첫 페이지 조회, AI 분석이 아직 진행중인 영상",
                                                    value = """
                                                            {
                                                              "timeStamp": "2025-11-07T18:30:00",
                                                              "message": "동영상 검색 완료",
                                                              "data": {
                                                                "results": [
                                                                  {
                                                                    "video": {
                                                                      "id": "abc123XYZ",
                                                                      "title": "최신 업로드 영상",
                                                                      "description": "방금 올라온 영상입니다...",
                                                                      "publishedAt": "2025-11-07T09:00:00Z",
                                                                      "thumbnailUrl": "https://i.ytimg.com/vi/abc123XYZ/maxresdefault.jpg",
                                                                      "viewCount": 1000,
                                                                      "likeCount": 50,
                                                                      "commentCount": 10
                                                                    },
                                                                    "channel": {
                                                                      "id": "UCabc123",
                                                                      "title": "테스트 채널",
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
                                                                      "id": "xyz789ABC",
                                                                      "title": "두 번째 페이지 영상",
                                                                      "description": "다음 페이지의 영상입니다...",
                                                                      "publishedAt": "2025-11-06T15:00:00Z",
                                                                      "thumbnailUrl": "https://i.ytimg.com/vi/xyz789ABC/maxresdefault.jpg",
                                                                      "viewCount": 50000,
                                                                      "likeCount": 2000,
                                                                      "commentCount": 300
                                                                    },
                                                                    "channel": {
                                                                      "id": "UCxyz789",
                                                                      "title": "다른 채널",
                                                                      "thumbnailUrl": "https://yt3.ggpht.com/...",
                                                                      "subscriberCount": 100000
                                                                    },
                                                                    "analysis": {
                                                                      "summary": "이 영상은...",
                                                                      "sentimentDistribution": {
                                                                        "positive": 0.70,
                                                                        "negative": 0.15,
                                                                        "other": 0.15
                                                                      },
                                                                      "keywords": ["튜토리얼", "가이드"]
                                                                    }
                                                                  }
                                                                ],
                                                                "nextPageToken": "CBQQAA",
                                                                "totalResults": 2,
                                                                "hasMore": true
                                                              }
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "마지막 페이지",
                                                    summary = "더 이상 결과가 없는 경우",
                                                    value = """
                                                            {
                                                              "timeStamp": "2025-11-07T18:30:00",
                                                              "message": "동영상 검색 완료",
                                                              "data": {
                                                                "results": [
                                                                  {
                                                                    "video": {
                                                                      "id": "last123",
                                                                      "title": "마지막 영상",
                                                                      "description": "검색 결과의 마지막 영상입니다",
                                                                      "publishedAt": "2025-11-05T12:00:00Z",
                                                                      "thumbnailUrl": "https://i.ytimg.com/vi/last123/maxresdefault.jpg",
                                                                      "viewCount": 10000,
                                                                      "likeCount": 500,
                                                                      "commentCount": 50
                                                                    },
                                                                    "channel": {
                                                                      "id": "UClast123",
                                                                      "title": "마지막 채널",
                                                                      "thumbnailUrl": "https://yt3.ggpht.com/...",
                                                                      "subscriberCount": 50000
                                                                    },
                                                                    "analysis": null
                                                                  }
                                                                ],
                                                                "nextPageToken": null,
                                                                "totalResults": 15,
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
                            description = "잘못된 요청 - 검색어가 비어있거나 유효하지 않음",
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
                            description = "서버 오류 - YouTube API 오류 또는 내부 서버 오류",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                        "httpStatus": "INTERNAL_SERVER_ERROR",
                                                        "message": "검색 중 오류 발생",
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
                    description = "JWT 토큰 (선택사항) - 로그인 시 스크랩 여부 포함",
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
                    example = "코딩 튜토리얼"
            ),
            @Parameter(
                    name = "pageToken",
                    description = "페이지네이션 토큰 (다음 페이지 조회 시 사용, 첫 페이지는 생략)",
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
            description = "YouTube 쇼츠를 검색합니다. (60초 미만의 세로형 영상)\n\n" +
                    "**Fast Path 아키텍처:**\n" +
                    "- 캐시된 데이터를 즉시 반환 (0.1초 이내)\n" +
                    "- 백그라운드에서 AI 분석 비동기 처리\n" +
                    "- AI 분석이 완료되지 않은 경우 기본값(null) 반환\n\n" +
                    "**무한 스크롤 지원:**\n" +
                    "- `nextPageToken`을 사용하여 다음 페이지 조회\n" +
                    "- `hasMore`로 추가 데이터 존재 여부 확인\n\n" +
                    "**응답 데이터:**\n" +
                    "- 쇼츠 기본 정보 (제목, 조회수, 좋아요 등)\n" +
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
                                                                      "title": "재미있는 쇼츠 #shorts",
                                                                      "description": "짧은 영상입니다",
                                                                      "publishedAt": "2025-11-07T10:00:00Z",
                                                                      "thumbnailUrl": "https://i.ytimg.com/vi/shorts123/maxresdefault.jpg",
                                                                      "viewCount": 500000,
                                                                      "likeCount": 25000,
                                                                      "commentCount": 1500
                                                                    },
                                                                    "channel": {
                                                                      "id": "UCshorts123",
                                                                      "title": "쇼츠 크리에이터",
                                                                      "thumbnailUrl": "https://yt3.ggpht.com/...",
                                                                      "subscriberCount": 200000
                                                                    },
                                                                    "analysis": {
                                                                      "summary": "재미있는 쇼츠 콘텐츠...",
                                                                      "sentimentDistribution": {
                                                                        "positive": 0.90,
                                                                        "negative": 0.03,
                                                                        "other": 0.07
                                                                      },
                                                                      "keywords": ["재미", "엔터테인먼트", "바이럴"]
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
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "서버 오류",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    @Parameters({
            @Parameter(
                    name = "Authorization",
                    description = "JWT 토큰 (선택사항) - 로그인 시 스크랩 여부 포함",
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
                    example = "댄스 챌린지"
            ),
            @Parameter(
                    name = "pageToken",
                    description = "페이지네이션 토큰 (다음 페이지 조회 시 사용, 첫 페이지는 생략)",
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
                    "**응답 데이터:**\n" +
                    "- 채널 기본 정보 (이름, 핸들, 설명 등)\n" +
                    "- 구독자 수\n" +
                    "- 관심 채널 여부 (로그인 사용자만)",
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
                                                        "message": "채널 검색 중 오류 발생",
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