package com.knu.sosuso.capstone.dto.response;

public interface OAuth2Response {
    // 제공자 (ex. google, naver, kakao, ...)
    String getProvider();

    // 제공자에서 발급해주는 아이디(번호)
    String getProviderId();

    // 이메일
    String getEmail();

    // 사용자 실명(설정한 이름)
    String getName();

    // 프로필 사진
    String getPicture();
}
