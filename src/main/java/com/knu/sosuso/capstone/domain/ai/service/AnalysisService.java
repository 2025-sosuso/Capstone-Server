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
            log.info("FastAPI로 AI 분석 요청: url={}", FASTAPI_URL);

            ResponseEntity<AIAnalysisResponse> aiAnalysisResponse = restTemplate.postForEntity(
                    FASTAPI_URL,
                    entity,
                    AIAnalysisResponse.class
            );

            log.info("FastAPI 응답 수신 상태: {}", aiAnalysisResponse.getStatusCode());

            if (aiAnalysisResponse.getStatusCode() == HttpStatus.OK && aiAnalysisResponse.getBody() != null) {
                return aiAnalysisResponse.getBody();
            } else {
                log.error("FastAPI 요청 실패: status={}", aiAnalysisResponse.getStatusCode());
                throw new BusinessException(AIError.AI_ANALYSIS_REQUEST_FAILED);
            }

        } catch (BusinessException e) {
            throw e;

        } catch (Exception e) {
            log.error("FastAPI 요청 중 예외 발생: error={}", e.getMessage(), e);
            throw new BusinessException(AIError.FASTAPI_CONNECTION_ERROR);
        }
    }
}