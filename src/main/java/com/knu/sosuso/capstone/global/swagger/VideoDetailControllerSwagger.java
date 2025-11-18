package com.knu.sosuso.capstone.global.swagger;

import com.knu.sosuso.capstone.domain.comment.dto.CommentDto;
import com.knu.sosuso.capstone.domain.detail.dto.*;
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
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Tag(
        name = "영상 상세 페이지 API",
        description = "YouTube 영상의 상세 정보를 섹션별로 제공하는 API입니다. " +
                "Progressive Loading을 위해 기본 정보, 분석, 댓글, AI 결과를 독립적으로 조회할 수 있습니다."
)
public interface VideoDetailControllerSwagger {

    @Operation(
            summary = "영상 기본 정보 조회",
            description = "YouTube 영상과 채널의 기본 정보를 제공합니다.\n\n" +
                    "**제공 정보:**\n" +
                    "- 영상: 제목, 설명, 썸네일, 조회수, 좋아요, 댓글 수, 업로드 날짜\n" +
                    "- 채널: 채널명, 썸네일, 구독자 수\n" +
                    "- 사용자 데이터: 스크랩 ID, 관심 채널 ID (로그인 시)\n\n" +
                    "**특징:** 가장 빠른 응답 (0.5-1초)",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "기본 정보 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseDto.class),
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "timeStamp": "2025-01-20T10:00:00",
                                                      "message": "영상 기본 정보 조회 성공",
                                                      "data": {
                                                        "video": {
                                                          "id": "dQw4w9WgXcQ",
                                                          "title": "영상 제목",
                                                          "description": "영상 설명...",
                                                          "publishedAt": "2025-01-15T00:00:00Z",
                                                          "thumbnailUrl": "https://...",
                                                          "viewCount": 1000000,
                                                          "likeCount": 50000,
                                                          "commentCount": 3000,
                                                          "scrapId": 123
                                                        },
                                                        "channel": {
                                                          "id": "UCxxxxxxxx",
                                                          "title": "채널명",
                                                          "thumbnailUrl": "https://...",
                                                          "subscriberCount": 500000,
                                                          "favoriteChannelId": 456
                                                        }
                                                      }
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "영상을 찾을 수 없음",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @Parameters({
            @Parameter(
                    name = "Authorization",
                    description = "JWT 토큰 (Cookie) - 선택사항",
                    required = false,
                    in = ParameterIn.COOKIE,
                    schema = @Schema(type = "string", format = "jwt")
            ),
            @Parameter(
                    name = "apiVideoId",
                    description = "YouTube 영상 ID",
                    required = true,
                    in = ParameterIn.PATH,
                    example = "dQw4w9WgXcQ"
            )
    })
    @ErrorCode400
    @ErrorCode500
    ResponseEntity<ResponseDto<VideoBasicResponse>> getVideoBasic(
            @CookieValue(value = "Authorization", required = false) String token,
            @PathVariable String apiVideoId
    );

    @Operation(
            summary = "영상 분석 정보 조회",
            description = "백엔드에서 분석한 댓글 데이터를 제공합니다.\n\n" +
                    "**제공 정보:**\n" +
                    "- 댓글 히스토그램 (시간대별 0-23시 분포)\n" +
                    "- 인기 타임스탬프 (댓글에서 가장 많이 언급된 시간대 TOP 5)\n" +
                    "- 좋아요 TOP 5 댓글\n" +
                    "- 감정 흐름 (시간 구간별 감정 비율 변화)\n\n" +
                    "**특징:** 인증 불필요, DB 조회만 수행 (1-2초)",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "분석 정보 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseDto.class),
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "timeStamp": "2025-01-20T10:00:01",
                                                      "message": "영상 분석 정보 조회 성공",
                                                      "data": {
                                                        "commentHistogram": [
                                                          {"hour": "0", "count": 10},
                                                          {"hour": "1", "count": 15},
                                                          {"hour": "14", "count": 150},
                                                          {"hour": "23", "count": 8}
                                                        ],
                                                        "popularTimestamps": [
                                                          {"time": "1:30", "mentionCount": 50},
                                                          {"time": "5:30", "mentionCount": 38},
                                                          {"time": "10:15", "mentionCount": 25}
                                                        ],
                                                        "topComments": [
                                                          {
                                                            "id": "comment1",
                                                            "author": "사용자1",
                                                            "text": "정말 유익한 영상이네요!",
                                                            "likeCount": 150,
                                                            "sentiment": "POSITIVE",
                                                            "publishedAt": "2025-01-15T10:00:00Z",
                                                            "hasReplies": false,
                                                            "detailSentiments": ["JOY", "GRATITUDE"]
                                                          },
                                                          {
                                                            "id": "comment2",
                                                            "author": "사용자2",
                                                            "text": "최고의 설명입니다",
                                                            "likeCount": 98,
                                                            "sentiment": "POSITIVE",
                                                            "publishedAt": "2025-01-15T11:30:00Z",
                                                            "hasReplies": false,
                                                            "detailSentiments": ["LOVE"]
                                                          }
                                                        ],
                                                        "sentimentFlow": [
                                                          {
                                                            "date": "2025-01-15",
                                                            "positive": 68,
                                                            "negative": 12,
                                                            "other": 20
                                                          },
                                                          {
                                                            "date": "2025-01-16",
                                                            "positive": 72,
                                                            "negative": 10,
                                                            "other": 18
                                                          },
                                                          {
                                                            "date": "2025-01-17",
                                                            "positive": 70,
                                                            "negative": 15,
                                                            "other": 15
                                                          }
                                                        ]
                                                      }
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    @Parameter(
            name = "apiVideoId",
            description = "YouTube 영상 ID",
            required = true,
            in = ParameterIn.PATH,
            example = "dQw4w9WgXcQ"
    )
    @ErrorCode400
    @ErrorCode500
    ResponseEntity<ResponseDto<VideoAnalysisResponse>> getVideoAnalysis(
            @PathVariable String apiVideoId
    );

    @Operation(
            summary = "전체 댓글 조회",
            description = "영상의 전체 댓글 목록을 제공합니다.\n\n" +
                    "**제공 정보:**\n" +
                    "- 최대 100개 댓글 (관련도순)\n" +
                    "- 댓글 내용, 작성자, 좋아요, 감정 분석 결과, 상세 감정, 작성 시간\n\n" +
                    "**특징:**\n" +
                    "- 인증 불필요\n" +
                    "- 페이징 없음 (한 번에 전체 반환)\n" +
                    "- 클라이언트에서 검색/필터링 가능",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "전체 댓글 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseDto.class),
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "timeStamp": "2025-01-20T10:00:02",
                                                      "message": "전체 댓글 조회 성공",
                                                      "data": [
                                                        {
                                                          "id": "comment1",
                                                          "author": "사용자1",
                                                          "text": "정말 유익한 영상이네요!",
                                                          "likeCount": 150,
                                                          "sentiment": "POSITIVE",
                                                          "publishedAt": "2025-01-15T10:00:00Z",
                                                          "hasReplies": true,
                                                          "detailSentiments": ["JOY", "GRATITUDE"]
                                                        },
                                                        {
                                                          "id": "comment2",
                                                          "author": "사용자2",
                                                          "text": "설명이 부족한 것 같아요",
                                                          "likeCount": 25,
                                                          "sentiment": "NEGATIVE",
                                                          "publishedAt": "2025-01-15T11:00:00Z",
                                                          "hasReplies": false,
                                                          "detailSentiments": ["SADNESS"]
                                                        },
                                                        {
                                                          "id": "comment3",
                                                          "author": "사용자3",
                                                          "text": "다음 영상도 기대됩니다",
                                                          "likeCount": 88,
                                                          "sentiment": "POSITIVE",
                                                          "publishedAt": "2025-01-15T12:30:00Z",
                                                          "hasReplies": true,
                                                          "detailSentiments": ["LOVE", "JOY"]
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
            name = "apiVideoId",
            description = "YouTube 영상 ID",
            required = true,
            in = ParameterIn.PATH,
            example = "dQw4w9WgXcQ"
    )
    @ErrorCode400
    @ErrorCode500
    ResponseEntity<ResponseDto<List<CommentDto>>> getVideoComments(
            @PathVariable String apiVideoId
    );

    @Operation(
            summary = "AI 분석 결과 조회",
            description = "FastAPI를 통해 분석된 AI 결과를 제공합니다.\n\n" +
                    "**제공 정보:**\n" +
                    "- AI 요약문\n" +
                    "- 경고 여부\n" +
                    "- 언어 분포 (한국어, 영어 등) - 백분율 정수\n" +
                    "- 감정 분포 (긍정, 부정, 기타) - 백분율 정수\n" +
                    "- 키워드 TOP 5\n\n" +
                    "**특징:**\n" +
                    "- 인증 불필요\n" +
                    "- AI 미완료 시 빈 데이터 반환 (summary=null)\n" +
                    "- 백그라운드 AI 처리 중일 수 있음 (Polling 권장)\n\n" +
                    "**변경사항:** 감정/언어 비율이 백분율 정수로 변경됨 (70, 20, 10)",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "AI 분석 결과 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseDto.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "AI 분석 완료",
                                                    value = """
                                                            {
                                                              "timeStamp": "2025-01-20T10:00:05",
                                                              "message": "AI 분석 결과 조회 성공",
                                                              "data": {
                                                                "summary": "이 영상은 블랙홀의 형성 과정과 최신 연구 결과를 소개합니다. 시청자들은 과학적 설명이 명확하고 시각 자료가 이해하기 쉽다는 긍정적 반응을 보였습니다. 일부는 더 깊이 있는 내용을 원한다는 의견도 있었습니다.",
                                                                "isWarning": false,
                                                                "languageDistribution": [
                                                                  {"language": "ko", "ratio": 85},
                                                                  {"language": "en", "ratio": 15}
                                                                ],
                                                                "sentimentDistribution": {
                                                                  "positive": 70,
                                                                  "negative": 12,
                                                                  "other": 18
                                                                },
                                                                "keywords": ["블랙홀", "우주", "과학", "중력파", "연구"]
                                                              }
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "AI 분석 미완료 (처리 중)",
                                                    value = """
                                                            {
                                                              "timeStamp": "2025-01-20T10:00:03",
                                                              "message": "AI 분석 결과 조회 성공",
                                                              "data": {
                                                                "summary": null,
                                                                "isWarning": false,
                                                                "languageDistribution": [],
                                                                "sentimentDistribution": {
                                                                  "positive": 0,
                                                                  "negative": 0,
                                                                  "other": 0
                                                                },
                                                                "keywords": []
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
            name = "apiVideoId",
            description = "YouTube 영상 ID",
            required = true,
            in = ParameterIn.PATH,
            example = "dQw4w9WgXcQ"
    )
    @ErrorCode400
    @ErrorCode500
    ResponseEntity<ResponseDto<AIAnalysisResponse>> getAIAnalysis(
            @PathVariable String apiVideoId
    );
}