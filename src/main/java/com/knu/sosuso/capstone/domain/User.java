package com.knu.sosuso.capstone.domain;

import com.knu.sosuso.capstone.dto.request.AuthRequest;
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

    public static User signUp(AuthRequest authRequest) {
        return User.builder()
                .sub(authRequest.sub())
                .email(authRequest.email())
                .name(authRequest.name())
                .role(authRequest.role())
                .picture(authRequest.picture())
                .build();
    }

    public void signIn(AuthRequest authRequest) {
        this.email = authRequest.email();
        this.name = authRequest.name();
        this.picture = authRequest.picture();
    }
}
