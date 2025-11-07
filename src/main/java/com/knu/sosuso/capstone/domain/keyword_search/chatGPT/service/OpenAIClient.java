package com.knu.sosuso.capstone.domain.keyword_search.chatGPT.service;

import com.knu.sosuso.capstone.domain.keyword_search.chatGPT.dto.request.ChatGPTRequest;
import com.knu.sosuso.capstone.domain.keyword_search.chatGPT.dto.response.ChatGPTResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class OpenAIClient {

    @Value("${openai.model}")
    private String model;

    @Value("${openai.api.url}")
    private String apiURL;

    @Value("${openai.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public String chatCompletion(String prompt) {
        ChatGPTRequest body = new ChatGPTRequest(model, prompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<ChatGPTRequest> entity = new HttpEntity<>(body, headers);

        ResponseEntity<ChatGPTResponse> res =
                restTemplate.exchange(apiURL, HttpMethod.POST, entity, ChatGPTResponse.class);

        ChatGPTResponse data = res.getBody();
        if (data == null || data.getChoices() == null || data.getChoices().isEmpty()) {
            throw new IllegalStateException("OpenAI empty response");
        }
        return data.getChoices().get(0).getMessage().getContent();
    }
}
