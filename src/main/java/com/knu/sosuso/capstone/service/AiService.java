package com.knu.sosuso.capstone.service;

import com.knu.sosuso.capstone.config.AiConfig;
import com.knu.sosuso.capstone.dto.AIRequest;
import com.knu.sosuso.capstone.dto.AIResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class AiService {
    private final RestTemplate restTemplate;
    private final AiConfig aiConfig;

    public AiService(RestTemplate restTemplate, AiConfig aiConfig) {
        this.restTemplate = restTemplate;
        this.aiConfig = aiConfig;
    }

    public AIResponse analyzeComments(List<String> commentTexts) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        AIRequest payload = new AIRequest(commentTexts);
        HttpEntity<AIRequest> request = new HttpEntity<>(payload, headers);

        return restTemplate.postForObject(
                aiConfig.getUrl() + "/analyze",
                request,
                AIResponse.class
        );
    }
}
