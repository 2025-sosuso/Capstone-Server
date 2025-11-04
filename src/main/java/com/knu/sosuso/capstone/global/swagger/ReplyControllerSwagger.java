package com.knu.sosuso.capstone.global.swagger;

import com.knu.sosuso.capstone.domain.comment.dto.response.ReplyResponse;
import com.knu.sosuso.capstone.global.ResponseDto;
import com.knu.sosuso.capstone.global.swagger.annotation.ErrorCode400;
import com.knu.sosuso.capstone.global.swagger.annotation.ErrorCode500;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(
        name = "대댓글 API",
        description = "YouTube 댓글의 대댓글(replies)을 조회하는 API입니다."
)
public interface ReplyControllerSwagger {

    @Operation(
            summary = "대댓글 조회",
            description = "특정 댓글(parentId)에 대한 대댓글 목록을 YouTube API를 통해 조회합니다. (최대 10개)",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "대댓글 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseDto.class),
                                    examples = @ExampleObject(
                                            name = "대댓글 조회 성공",
                                            value = """
                                                    {
                                                      "timeStamp": "2025-06-09T18:00:00",
                                                      "message": "대댓글 조회 성공",
                                                      "data": {
                                                        "apiCommentId": "UgwS-cv7fcHT8U1wQvJ4AaABAg",
                                                        "replies": [
                                                          {
                                                            "id": "UgwS-cv7fcHT8U1wQvJ4AaABAg.1_reply_id",
                                                            "author": "대댓글 작성자",
                                                            "text": "정말 공감합니다!",
                                                            "likeCount": 5,
                                                            "publishedAt": "2025-06-09T10:30:00Z"
                                                          }
                                                        ]
                                                      }
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "대댓글 조회 실패 (YouTube API 오류 등)",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "조회 실패",
                                            value = """
                                                    {
                                                        "httpStatus": "INTERNAL_SERVER_ERROR",
                                                        "message": "대댓글 조회 실패",
                                                        "timeStamp": "2025-06-09T18:00:00"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    @Parameter(
            name = "apiCommentId",
            description = "대댓글을 조회할 부모 댓글의 ID",
            required = true,
            in = ParameterIn.PATH,
            schema = @Schema(type = "string"),
            example = "UgwS-cv7fcHT8U1wQvJ4AaABAg"
    )
    @ErrorCode400
    @ErrorCode500
    ResponseEntity<ResponseDto<ReplyResponse>> getReplies(
            @PathVariable String apiCommentId
    );
}