package com.knu.sosuso.capstone.domain.keyword_search.chatGPT.dto.request;


import com.knu.sosuso.capstone.domain.keyword_search.chatGPT.dto.Message;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatGPTRequest {

    private final String model;
    private final List<Message> messages;

    public ChatGPTRequest(String model, String prompt) {
        this.model = model;
        this.messages = new ArrayList<>();
        this.messages.add(new Message("user", prompt));
    }
}
