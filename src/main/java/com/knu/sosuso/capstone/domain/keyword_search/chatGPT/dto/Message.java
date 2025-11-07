package com.knu.sosuso.capstone.domain.keyword_search.chatGPT.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private String role;
    private String content;
}
