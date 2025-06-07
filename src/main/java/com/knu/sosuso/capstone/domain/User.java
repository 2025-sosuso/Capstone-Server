package com.knu.sosuso.capstone.domain;

import com.knu.sosuso.capstone.security.GoogleUserInfo;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

@Getter
@Table(name = "user")
@Entity
public class User extends BaseEntity {

    @Column(name = "sub")
    private String sub;

    @Column(name = "email")
    private String email;

    @Column(name = "name")
    private String name;

    @Column(name = "role")
    private String role;

    @Column(name = "picture")
    private String picture;

    protected User(){
    }

    @Builder
    public User(String sub, String email, String name, String role, String picture) {
        this.sub = sub;
        this.email = email;
        this.name = name;
        this.role = role;
        this.picture = picture;
    }

    public static User signUp(GoogleUserInfo googleUserInfo) {
        return User.builder()
                .sub(googleUserInfo.sub())
                .email(googleUserInfo.email())
                .name(googleUserInfo.name())
                .role(googleUserInfo.role())
                .picture(googleUserInfo.picture())
                .build();
    }

    public void signIn(GoogleUserInfo googleUserInfo) {
        this.email = googleUserInfo.email();
        this.name = googleUserInfo.name();
        this.picture = googleUserInfo.picture();
    }
}
