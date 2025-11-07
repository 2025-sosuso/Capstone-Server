package com.knu.sosuso.capstone.domain.keyword_search.chatGPT.dto.response;

import com.knu.sosuso.capstone.domain.keyword_search.chatGPT.dto.Choice;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatGPTResponse {
    private List<Choice> choices;
}
