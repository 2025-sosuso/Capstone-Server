package com.knu.sosuso.capstone.controller;

import com.knu.sosuso.capstone.dto.ResponseDto;
import com.knu.sosuso.capstone.dto.request.CreateScrapRequest;
import com.knu.sosuso.capstone.dto.response.CreateScrapResponse;
import com.knu.sosuso.capstone.service.ScrapService;
import com.knu.sosuso.capstone.swagger.ScrapControllerSwagger;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RequestMapping("/api/scraps")
@RestController
public class ScrapController implements ScrapControllerSwagger {

    private final ScrapService scrapService;

    @PostMapping()
    public ResponseDto<CreateScrapResponse> createScrap(
            @CookieValue("Authorization") String token,
            @RequestBody @Valid CreateScrapRequest createScrapRequest
    ) {
        CreateScrapResponse createScrapResponse = scrapService.createScrap(token, createScrapRequest);
        return ResponseDto.of(createScrapResponse, "Successfully created Scrap");
    }

    @DeleteMapping("{id}")
    public ResponseDto<?> cancelScrap(
            @CookieValue("Authorization") String token,
            @PathVariable("id") Long scrapId
    ) {
        scrapService.cancelScrap(token, scrapId);
        return ResponseDto.of("Successfully canceled the scrap.");
    }
}
