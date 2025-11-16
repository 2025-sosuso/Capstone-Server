package com.knu.sosuso.capstone.domain.ai.service;

import com.knu.sosuso.capstone.domain.ai.dto.AIAnalysisRequest;
import com.knu.sosuso.capstone.domain.ai.dto.AIAnalysisResponse;
import com.knu.sosuso.capstone.global.exception.BusinessException;
import com.knu.sosuso.capstone.global.exception.error.AIError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RequiredArgsConstructor
@Service
public class AnalysisService {

    private final RestTemplate restTemplate;

    @Value("${fastapi.url}")
    private String FASTAPI_URL;

    /**
     * AI에 분석 요청
     */
    public AIAnalysisResponse requestAnalysis(AIAnalysisRequest aiAnalysisRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<AIAnalysisRequest> entity = new HttpEntity<>(aiAnalysisRequest, headers);

        try {
            log.info("FastAPI AI 분석 요청: url={}, videoId={}, 댓글 수={}",
                    FASTAPI_URL, aiAnalysisRequest.videoId(), aiAnalysisRequest.comments().size());

            long startTime = System.currentTimeMillis();

            ResponseEntity<AIAnalysisResponse> aiAnalysisResponse = restTemplate.postForEntity(
                    FASTAPI_URL,
                    entity,
                    AIAnalysisResponse.class
            );

            long responseTime = System.currentTimeMillis() - startTime;

            log.info("FastAPI 응답 수신: status={}, videoId={}, 응답시간={}ms",
                    aiAnalysisResponse.getStatusCode(), aiAnalysisRequest.videoId(), responseTime);

            if (aiAnalysisResponse.getStatusCode() == HttpStatus.OK && aiAnalysisResponse.getBody() != null) {
                AIAnalysisResponse body = aiAnalysisResponse.getBody();
                log.info("AI 응답 데이터: videoId={}, summation={}, keywords={}, sentiments={}",
                        aiAnalysisRequest.videoId(),
                        body.summation() != null ? body.summation().length() + "자" : "null",
                        body.keywords() != null ? body.keywords().size() + "개" : "null",
                        body.sentimentRatio() != null ? "있음" : "null");

                return body;
            } else {
                log.error("FastAPI 요청 실패: status={}, videoId={}, body={}",
                        aiAnalysisResponse.getStatusCode(),
                        aiAnalysisRequest.videoId(),
                        aiAnalysisResponse.getBody());
                throw new BusinessException(AIError.AI_ANALYSIS_REQUEST_FAILED);
            }

        } catch (BusinessException e) {
            throw e;

        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("FastAPI 연결 실패 (네트워크): url={}, videoId={}, error={}",
                    FASTAPI_URL, aiAnalysisRequest.videoId(), e.getMessage());
            throw new BusinessException(AIError.FASTAPI_CONNECTION_ERROR);

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("FastAPI 클라이언트 에러: url={}, videoId={}, status={}, error={}",
                    FASTAPI_URL, aiAnalysisRequest.videoId(), e.getStatusCode(), e.getMessage());
            throw new BusinessException(AIError.AI_ANALYSIS_REQUEST_FAILED);

        } catch (org.springframework.web.client.HttpServerErrorException e) {
            log.error("FastAPI 서버 에러: url={}, videoId={}, status={}, error={}",
                    FASTAPI_URL, aiAnalysisRequest.videoId(), e.getStatusCode(), e.getMessage());
            throw new BusinessException(AIError.FASTAPI_CONNECTION_ERROR);

        } catch (Exception e) {
            log.error("FastAPI 요청 중 예상치 못한 예외: url={}, videoId={}, error={}",
                    FASTAPI_URL, aiAnalysisRequest.videoId(), e.getMessage(), e);
            throw new BusinessException(AIError.FASTAPI_CONNECTION_ERROR);
        }
    }
}