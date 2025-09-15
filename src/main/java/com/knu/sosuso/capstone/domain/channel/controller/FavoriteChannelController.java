package com.knu.sosuso.capstone.domain.channel.controller;

import com.knu.sosuso.capstone.global.ResponseDto;
import com.knu.sosuso.capstone.domain.channel.dto.request.RegisterFavoriteChannelRequest;
import com.knu.sosuso.capstone.domain.channel.dto.response.CancelFavoriteChannelResponse;
import com.knu.sosuso.capstone.domain.channel.dto.response.FavoriteChannelListResponse;
import com.knu.sosuso.capstone.domain.channel.dto.response.FavoriteVideoInfoResponse;
import com.knu.sosuso.capstone.domain.channel.dto.response.RegisterFavoriteChannelResponse;
import com.knu.sosuso.capstone.domain.channel.service.FavoriteChannelService;
import com.knu.sosuso.capstone.global.swagger.FavoriteChannelControllerSwagger;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/favorite-channels")
@RestController
public class FavoriteChannelController implements FavoriteChannelControllerSwagger {

    private final FavoriteChannelService favoriteChannelService;

    @PostMapping()
    public ResponseDto<RegisterFavoriteChannelResponse> registerFavoriteChannel(
            @CookieValue("Authorization") String token,
            @RequestBody @Valid RegisterFavoriteChannelRequest registerFavoriteChannelRequest
            ) {
        RegisterFavoriteChannelResponse registerFavoriteChannelResponse = favoriteChannelService.registerFavoriteChannel(token, registerFavoriteChannelRequest);
        return ResponseDto.of(registerFavoriteChannelResponse, "Successfully registered the favorite channel.");
    }

    @GetMapping()
    public ResponseDto<List<FavoriteChannelListResponse>> favoriteChannelList(
            @CookieValue("Authorization") String token
    ) {
        List<FavoriteChannelListResponse> favoriteChannelListResponses = favoriteChannelService.getFavoriteChannelList(token);
        return ResponseDto.of(favoriteChannelListResponses, "successfully retrieved your list of favorite channels.");
    }

    @DeleteMapping("{channelId}")
    public ResponseDto<CancelFavoriteChannelResponse> cancelFavoriteChannel(
            @CookieValue("Authorization") String token,
            @PathVariable("channelId") Long channelId
    ) {
        CancelFavoriteChannelResponse cancelFavoriteChannelResponse = favoriteChannelService.cancelFavoriteChannel(token, channelId);
        return ResponseDto.of(cancelFavoriteChannelResponse, "successfully canceled the favorite channel.");
    }

    @GetMapping("{apiChannelId}")
    public ResponseDto<FavoriteVideoInfoResponse> favoriteChannelVideo(
            @CookieValue("Authorization") String token,
            @PathVariable(value = "apiChannelId") String apiChannelId
    ){
        FavoriteVideoInfoResponse favoriteChannelSummaryResponse = favoriteChannelService.processLatestVideoFromFavoriteChannel(token, apiChannelId);
        return ResponseDto.of(favoriteChannelSummaryResponse, "successfully summary analysis video favorite channel.");
    }

}
