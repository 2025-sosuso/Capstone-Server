package com.knu.sosuso.capstone.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Builder;

@Entity
public class Scrap extends BaseEntity {

    @JoinColumn(name = "user_id")
    @ManyToOne
    private User user;

    @JoinColumn(name = "video_id")
    @ManyToOne
    private Video video;

    @Column(name = "api_video_id")
    private String apiVideoId;

    protected Scrap(){
    }

    @Builder
    public Scrap(User user, Video video, String apiVideoId) {
        this.user = user;
        this.video = video;
        this.apiVideoId = apiVideoId;
    }
}
