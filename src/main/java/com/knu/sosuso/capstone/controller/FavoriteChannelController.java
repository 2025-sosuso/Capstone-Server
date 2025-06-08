package com.knu.sosuso.capstone.controller;

import com.knu.sosuso.capstone.dto.ResponseDto;
import com.knu.sosuso.capstone.dto.request.RegisterFavoriteChannelRequest;
import com.knu.sosuso.capstone.dto.response.favorite_channel.CancelFavoriteChannelResponse;
import com.knu.sosuso.capstone.dto.response.favorite_channel.RegisterFavoriteChannelResponse;
import com.knu.sosuso.capstone.service.FavoriteChannelService;
import com.knu.sosuso.capstone.swagger.FavoriteChannelControllerSwagger;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    @DeleteMapping("{id}")
    public ResponseDto<CancelFavoriteChannelResponse> cancelFavoriteChannel(
            @CookieValue("Authorization") String token,
            @PathVariable("id") Long channelId
    ) {
        CancelFavoriteChannelResponse cancelFavoriteChannelResponse = favoriteChannelService.cancelFavoriteChannel(token, channelId);
        return ResponseDto.of(cancelFavoriteChannelResponse, "successfully canceled the favorite channel.");
    }
}
