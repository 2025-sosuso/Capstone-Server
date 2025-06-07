package com.knu.sosuso.capstone.ai.service;

import com.knu.sosuso.capstone.ai.dto.AIAnalysisRequest;
import com.knu.sosuso.capstone.ai.dto.AIAnalysisResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@RequiredArgsConstructor
@Service
public class AnalysisService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String FASTAPI_URL = "https://7454-49-171-245-132.ngrok-free.app/analyze";

    /**
     * AI에 분석 요청
     * @param aiAnalysisRequest
     * @return
     */
    public AIAnalysisResponse requestAnalysis(AIAnalysisRequest aiAnalysisRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<AIAnalysisRequest> entity = new HttpEntity<>(aiAnalysisRequest, headers);

        ResponseEntity<AIAnalysisResponse> aiAnalysisResponse = restTemplate.postForEntity(
                FASTAPI_URL,
                entity,
                AIAnalysisResponse.class
        );

        if (aiAnalysisResponse.getStatusCode() == HttpStatus.OK && aiAnalysisResponse.getBody() != null) {
            return aiAnalysisResponse.getBody();
        } else {
            throw new RuntimeException("FastAPI request failed: " + aiAnalysisResponse.getStatusCode());
        }
    }
}
