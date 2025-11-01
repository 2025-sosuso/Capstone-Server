package com.knu.sosuso.capstone.domain.conmment.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.sosuso.capstone.domain.conmment.dto.ReplyDto;
import com.knu.sosuso.capstone.domain.conmment.dto.response.ReplyResponse;
import com.knu.sosuso.capstone.global.ResponseDto;
import com.knu.sosuso.capstone.global.config.ApiConfig;
import com.knu.sosuso.capstone.global.exception.BusinessException;
import com.knu.sosuso.capstone.global.exception.error.CommonError;
import com.knu.sosuso.capstone.global.swagger.ReplyControllerSwagger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Slf4j
public class ReplyController implements ReplyControllerSwagger {

    private final ApiConfig apiConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/{apiCommentId}/replies")
    public ResponseEntity<ResponseDto<ReplyResponse>> getReplies(@PathVariable String apiCommentId) {
        try {
            // YouTube API 호출
            String url = "https://www.googleapis.com/youtube/v3/comments"
                    + "?part=snippet"
                    + "&parentId=" + apiCommentId
                    + "&maxResults=10"
                    + "&textFormat=plainText"
                    + "&key=" + apiConfig.getKey();

            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(json);
            JsonNode items = root.path("items");

            // 파싱
            List<ReplyDto> replies = new ArrayList<>();
            for (JsonNode item : items) {
                JsonNode snippet = item.path("snippet");
                replies.add(new ReplyDto(
                        item.path("id").asText(),
                        snippet.path("authorDisplayName").asText(),
                        snippet.path("textDisplay").asText(),
                        snippet.path("likeCount").asInt(0),
                        snippet.path("publishedAt").asText()
                ));
            }

            return ResponseEntity.ok(ResponseDto.of(
                    new ReplyResponse(apiCommentId, replies),
                    "대댓글 조회 성공"
            ));

        } catch (Exception e) {
            log.error("대댓글 조회 실패: apiCommentId={}, error={}", apiCommentId, e.getMessage());
            throw new BusinessException(CommonError.INTERNAL_SERVER_ERROR);
        }
    }
}