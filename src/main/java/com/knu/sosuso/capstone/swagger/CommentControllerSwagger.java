package com.knu.sosuso.capstone.swagger;

import com.knu.sosuso.capstone.dto.ResponseDto;
import com.knu.sosuso.capstone.dto.response.comment.CommentResponse;
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
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(
        name = "댓글 검색 API",
        description = "YouTube 영상의 상세페이지 댓글 검색/감정/키워드 필터링 API입니다. " +
                "일반 텍스트 검색, 감정별 필터링, AI 키워드 검색 기능을 제공합니다. "
)
public interface CommentControllerSwagger {
    @Operation(
            summary = "댓글 검색/감정/키워드 필터링",
            description = "특정 YouTube 영상의 댓글을 다양한 조건으로 검색하고 필터링합니다.\n\n" +
                    "**검색 타입 (단일 조건만 허용):**\n" +
                    "- **일반 텍스트 검색 (`q`)**: 댓글 내용에서 특정 단어나 문구를 검색\n" +
                    "- **감정별 필터링 (`sentiment`)**: 댓글의 감정 분석 결과를 기반으로 필터링\n" +
                    "- **AI 키워드 검색 (`keyword`)**: AI가 추출한 키워드를 기반으로 댓글 검색\n\n" +
                    "**감정 타입:**\n" +
                    "- `POSITIVE`: 긍정적인 댓글\n" +
                    "- `NEGATIVE`: 부정적인 댓글\n" +
                    "- `OTHER`: 중립적이거나 분류되지 않은 댓글\n\n" +
                    "**주의사항:**\n" +
                    "- 세 개의 파라미터 중 **정확히 하나만** 사용해야 합니다\n" +
                    "- 여러 조건을 동시에 사용하면 400 에러가 발생합니다\n" +
                    "- 아무 조건도 제공하지 않으면 400 에러가 발생합니다",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "댓글 검색 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseDto.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "일반 텍스트 검색 성공",
                                                    summary = "특정 단어로 댓글 검색한 결과",
                                                    value = """
                                                            {
                                                              "timeStamp": "2025-06-09T16:00:00",
                                                              "message": "'재미있다' 텍스트 검색이 완료되었습니다. (결과: 15개)",
                                                              "data": {
                                                                "apiVideoId": "dQw4w9WgXcQ",
                                                                "results": [
                                                                  {
                                                                    "id": "UgwS-cv7fcHT8U1wQvJ4AaABAg",
                                                                    "author": "김유저",
                                                                    "text": "정말 재미있다! 계속 보게 되네요",
                                                                    "likeCount": 42,
                                                                    "sentiment": "positive",
                                                                    "publishedAt": "2025-06-08T14:30:00Z"
                                                                  },
                                                                  {
                                                                    "id": "UgxKHjF2Mn1wcPdO_9J4AaABAg",
                                                                    "author": "박댓글러",
                                                                    "text": "이렇게 재미있다니 믿을 수 없어",
                                                                    "likeCount": 28,
                                                                    "sentiment": "positive",
                                                                    "publishedAt": "2025-06-08T16:45:00Z"
                                                                  }
                                                                ]
                                                              }
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "감정별 필터링 성공",
                                                    summary = "긍정적인 댓글만 필터링한 결과",
                                                    value = """
                                                            {
                                                              "timeStamp": "2025-06-09T16:00:00",
                                                              "message": "POSITIVE 감정 댓글 조회가 완료되었습니다. (결과: 127개)",
                                                              "data": {
                                                                "apiVideoId": "dQw4w9WgXcQ",
                                                                "results": [
                                                                  {
                                                                    "id": "UgwXYZ123abc456def789_AaA",
                                                                    "author": "행복한사용자",
                                                                    "text": "최고의 영상입니다! 👍👍👍",
                                                                    "likeCount": 156,
                                                                    "sentiment": "positive",
                                                                    "publishedAt": "2025-06-08T12:15:00Z"
                                                                  },
                                                                  {
                                                                    "id": "UgwABC987zyx654wvu321_BbB",
                                                                    "author": "좋아요맨",
                                                                    "text": "감동적이에요 ㅠㅠ 너무 좋아요",
                                                                    "likeCount": 89,
                                                                    "sentiment": "positive",
                                                                    "publishedAt": "2025-06-08T13:22:00Z"
                                                                  }
                                                                ]
                                                              }
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "AI 키워드 검색 성공",
                                                    summary = "AI 키워드로 검색한 결과",
                                                    value = """
                                                            {
                                                              "timeStamp": "2025-06-09T16:00:00",
                                                              "message": "'음악' 키워드 검색이 완료되었습니다. (결과: 23개)",
                                                              "data": {
                                                                "apiVideoId": "dQw4w9WgXcQ",
                                                                "results": [
                                                                  {
                                                                    "id": "UgwMUSIC456def789abc123",
                                                                    "author": "음악애호가",
                                                                    "text": "이런 음악이야말로 진짜 명곡이죠",
                                                                    "likeCount": 73,
                                                                    "sentiment": "positive",
                                                                    "publishedAt": "2025-06-08T11:30:00Z"
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
                            description = "잘못된 요청 - 검색 조건 오류",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "검색 조건 없음",
                                                    value = """
                                                            {
                                                              "httpStatus": "BAD_REQUEST",
                                                              "message": "검색 조건이 필요합니다. (q, keyword, sentiment 중 하나)",
                                                              "timeStamp": "2025-06-09T16:00:00"
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "중복 검색 조건",
                                                    value = """
                                                            {
                                                              "httpStatus": "BAD_REQUEST",
                                                              "message": "하나의 검색 조건만 사용할 수 있습니다.",
                                                              "timeStamp": "2025-06-09T16:00:00"
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "잘못된 감정 타입",
                                                    value = """
                                                            {
                                                              "httpStatus": "BAD_REQUEST",
                                                              "message": "유효하지 않은 감정 타입입니다: INVALID_TYPE",
                                                              "timeStamp": "2025-06-09T16:00:00"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "비디오를 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "비디오 없음 에러",
                                            value = """
                                                    {
                                                      "httpStatus": "NOT_FOUND",
                                                      "message": "비디오를 찾을 수 없습니다: invalid_video_id",
                                                      "timeStamp": "2025-06-09T16:00:00"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    @Parameters({
            @Parameter(
                    name = "apiVideoId",
                    description = "YouTube 영상 ID",
                    required = true,
                    in = ParameterIn.PATH,
                    schema = @Schema(type = "string"),
                    example = "dQw4w9WgXcQ"
            ),
            @Parameter(
                    name = "q",
                    description = "일반 텍스트 검색어 (댓글 내용에서 검색)",
                    required = false,
                    in = ParameterIn.QUERY,
                    schema = @Schema(type = "string"),
                    examples = {
                            @ExampleObject(name = "한글 검색", value = "재미있다"),
                            @ExampleObject(name = "영어 검색", value = "amazing"),
                            @ExampleObject(name = "이모지 포함", value = "👍")
                    }
            ),
            @Parameter(
                    name = "keyword",
                    description = "AI 키워드 검색어 (AI가 추출한 키워드 기반)",
                    required = false,
                    in = ParameterIn.QUERY,
                    schema = @Schema(type = "string"),
                    examples = {
                            @ExampleObject(name = "음악 관련", value = "음악"),
                            @ExampleObject(name = "게임 관련", value = "게임"),
                            @ExampleObject(name = "리뷰 관련", value = "리뷰")
                    }
            ),
            @Parameter(
                    name = "sentiment",
                    description = "감정 필터링 (댓글의 감정 분석 결과)",
                    required = false,
                    in = ParameterIn.QUERY,
                    schema = @Schema(
                            type = "string",
                            allowableValues = {"POSITIVE", "NEGATIVE", "OTHER"}
                    ),
                    examples = {
                            @ExampleObject(name = "긍정적 댓글", value = "POSITIVE"),
                            @ExampleObject(name = "부정적 댓글", value = "NEGATIVE"),
                            @ExampleObject(name = "중립적 댓글", value = "OTHER")
                    }
            )
    })
    @ErrorCode400
    @ErrorCode500
    ResponseEntity<ResponseDto<CommentResponse>> searchComments(
            @PathVariable String apiVideoId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sentiment
    );
}