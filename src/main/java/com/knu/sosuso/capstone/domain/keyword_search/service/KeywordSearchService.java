package com.knu.sosuso.capstone.domain.keyword_search.service;

import com.knu.sosuso.capstone.domain.keyword_search.chatGPT.service.OpenAIClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class KeywordSearchService {

    private final OpenAIClient openAIClient;

    public String getKeywordDescription(String keyword) {
        String prompt = buildConversationalPrompt(keyword);
        return openAIClient.chatCompletion(prompt);
    }

    private String buildConversationalPrompt(String keyword) {
        return """
                Explain the meaning of the following keyword in one concise Korean sentence.
                The keyword may be in any language (Korean, English, Japanese, French, etc.).
                Automatically detect its language, but always respond in Korean.
                The explanation should be short, objective, and end with "이에요."
                Do not include the keyword itself in the response.
                If you are not certain about the meaning, consider if it might be a slang or new term,
                and if still unsure, respond with "해당 단어의 정확한 의미를 알 수 없어요."
                
                Keyword: "%s"
                """.formatted(keyword);
    }
}
