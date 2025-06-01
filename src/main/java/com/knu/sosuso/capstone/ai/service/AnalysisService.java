package com.knu.sosuso.capstone.ai.service;

import com.knu.sosuso.capstone.ai.dto.AnalysisRequest;
import com.knu.sosuso.capstone.ai.dto.AnalysisResponse;
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
     * @param analysisRequest
     * @return
     */
    public AnalysisResponse requestAnalysis(AnalysisRequest analysisRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<AnalysisRequest> entity = new HttpEntity<>(analysisRequest, headers);

        ResponseEntity<AnalysisResponse> analysisResponse = restTemplate.postForEntity(
                FASTAPI_URL,
                entity,
                AnalysisResponse.class
        );

        if (analysisResponse.getStatusCode() == HttpStatus.OK && analysisResponse.getBody() != null) {
            return analysisResponse.getBody();
        } else {
            throw new RuntimeException("FastAPI request failed: " + analysisResponse.getStatusCode());
        }
    }
}
