package com.knu.sosuso.capstone.global.swagger;

import com.knu.sosuso.capstone.domain.video.dto.response.VideoSummaryResponse; // import 추가
import com.knu.sosuso.capstone.global.ResponseDto;
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

import java.util.List; // import 추가

@Tag(
        name = "인기 영상 API",
        description = "자체 알고리즘 기반 인기 영상 조회 API입니다. " +
                "최근 N일간(설정값 기준) 검색/조회 횟수와 스크랩 횟수를 기반으로 인기 영상을 선정합니다. " +
                "매 시간마다 자동으로 업데이트됩니다."
)
public interface TrendingControllerSwagger {
    @Operation(
            summary = "자체 알고리즘 기반 인기 영상 조회",
            description = "우리 서비스의 검색/조회 + 스크랩 데이터를 기반으로 인기 영상 TOP N을 조회합니다.\n\n" +
                    "**인기도 점수 계산 방식 (AppConfig 설정 기반):**\n" + // 설명 수정
                    "- 점수 = (최근 N일간 조회수 × 조회 가중치) + (전체 스크랩 횟수 × 스크랩 가중치)\n" +
                    "- 조회: URL 검색 + 상세 페이지 조회\n" +
                    "- 스크랩: 실제 저장 행동으로 더 높은 가중치 부여\n\n" +
                    "**업데이트 주기:**\n" +
                    "- 매 시간 정각에 자동으로 점수 재계산\n" +
                    "- 실시간성 있는 인기 영상 반영\n\n" +
                    "**제공 정보:**\n" +
                    "- 영상 기본 정보 (제목, 설명, 조회수, 좋아요 등)\n" +
                    "- 채널 정보 (채널명, 구독자 수)\n" +
                    "- AI 분석 결과 (요약, 감정 분포, 키워드)",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "인기 영상 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseDto.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "인기 영상 조회 성공",
                                                    summary = "인기 영상 조회 결과",
                                                    value = """
                                                            {
                                                              "timeStamp": "2025-11-02T15:30:00",
                                                              "message": "인기 영상 조회 성공",
                                                              "data": [
                                                                {
                                                                  "video": {
                                                                    "id": "abc123xyz",
                                                                    "title": "최고 인기 영상 제목",
                                                                    "description": "영상 설명입니다...",
                                                                    "publishedAt": "2025-10-28T12:00:00Z",
                                                                    "thumbnailUrl": "https://i.ytimg.com/vi/abc123xyz/maxresdefault.jpg",
                                                                    "viewCount": 2500000,
                                                                    "likeCount": 125000,
                                                                    "commentCount": 18500
                                                                  },
                                                                  "channel": {
                                                                    "id": "UC_channel_id",
                                                                    "title": "인기 채널",
                                                                    "thumbnailUrl": "https://yt3.ggpht.com/channel-thumbnail.jpg",
                                                                    "subscriberCount": 3500000
                                                                  },
                                                                  "analysis": {
                                                                    "summary": "이 영상은 최근 우리 서비스에서 가장 많이 검색되고 스크랩된 영상입니다. AI 분석 결과 긍정적인 반응이 많으며...",
                                                                    "sentimentDistribution": {
                                                                      "positive": 82,
                                                                      "negative": 10,
                                                                      "other": 8
                                                                    },
                                                                    "keywords": ["인기", "트렌드", "화제", "추천", "유익함"]
                                                                  }
                                                                },
                                                                {
                                                                  "video": {
                                                                    "id": "def456uvw",
                                                                    "title": "두 번째 인기 영상",
                                                                    "description": "이 영상도 많이 검색되었습니다...",
                                                                    "publishedAt": "2025-10-30T18:30:00Z",
                                                                    "thumbnailUrl": "https://i.ytimg.com/vi/def456uvw/maxresdefault.jpg",
                                                                    "viewCount": 1800000,
                                                                    "likeCount": 85000,
                                                                    "commentCount": 12500
                                                                  },
                                                                  "channel": {
                                                                    "id": "UC_popular_channel",
                                                                    "title": "인기 채널 2",
                                                                    "thumbnailUrl": "https://yt3.ggpht.com/channel2-thumbnail.jpg",
                                                                    "subscriberCount": 2200000
                                                                  },
                                                                  "analysis": {
                                                                    "summary": "이 영상은 댓글 분석 결과 긍정적 반응이 많은 영상입니다...",
                                                                    "sentimentDistribution": {
                                                                      "positive": 75,
                                                                      "negative": 15,
                                                                      "other": 10
                                                                    },
                                                                    "keywords": ["유익", "정보", "도움", "좋아요", "최고"]
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
                            responseCode = "400",
                            description = "잘못된 요청 - 잘못된 maxResults 값",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "잘못된 파라미터 에러",
                                            value = """
                                                    {
                                                        "httpStatus": "BAD_REQUEST",
                                                        "message": "잘못된 요청: maxResults는 1 이상 30 이하여야 합니다",
                                                        "timeStamp": "2025-11-02T15:30:00"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "서버 내부 오류",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "서버 오류",
                                            value = """
                                                    {
                                                        "httpStatus": "INTERNAL_SERVER_ERROR",
                                                        "message": "인기 영상 조회 중 오류가 발생했습니다.",
                                                        "timeStamp": "2025-11-02T15:30:00"
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
                    name = "maxResults",
                    description = "조회할 인기 영상 개수 (최대 30개, 기본값 10개)",
                    required = false,
                    in = ParameterIn.QUERY,
                    schema = @Schema(
                            type = "integer",
                            minimum = "1",
                            maximum = "30",
                            defaultValue = "10"
                    ),
                    example = "10"
            )
    })
    @ErrorCode400
    @ErrorCode500
    ResponseEntity<ResponseDto<List<VideoSummaryResponse>>> getPopularVideos(
            @CookieValue(value = "Authorization", required = false) String token,
            @RequestParam(defaultValue = "10") int maxResults
    );
}