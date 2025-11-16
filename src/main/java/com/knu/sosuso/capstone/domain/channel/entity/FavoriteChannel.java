package com.knu.sosuso.capstone.domain.channel.entity;

import com.knu.sosuso.capstone.global.BaseEntity;
import com.knu.sosuso.capstone.domain.auth.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "favorite_channel")
public class FavoriteChannel extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "api_channel_id")
    private String apiChannelId;

    @Column(name = "api_channel_name")
    private String apiChannelName;

    @Column(name = "api_channel_thumbnail")
    private String apiChannelThumbnail;
}
