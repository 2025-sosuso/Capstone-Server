package com.knu.sosuso.capstone.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

@Getter
@Entity
@Table(name = "favorite_channel")
public class FavoriteChannel extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "api_channel_id")
    private String apiChannelId;

    @Column(name = "api_channel_name")
    private String apiChannelName;

    protected FavoriteChannel() {
    }

    @Builder
    public FavoriteChannel(User user, String apiChannelId, String apiChannelName) {
        this.user = user;
        this.apiChannelId = apiChannelId;
        this.apiChannelName = apiChannelName;
    }
}
