package com.knu.sosuso.capstone.service;

import com.knu.sosuso.capstone.domain.User;
import com.knu.sosuso.capstone.dto.CustomOAuth2User;
import com.knu.sosuso.capstone.dto.request.AuthRequest;
import com.knu.sosuso.capstone.dto.response.GoogleResponse;
import com.knu.sosuso.capstone.dto.response.OAuth2Response;
import com.knu.sosuso.capstone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final String ROLE = "ROLE_USER";
    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        System.out.println("oAuth2User = " + oAuth2User);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2Response oAuth2Response;
        if (registrationId.equals("google")) {
            oAuth2Response = new GoogleResponse(oAuth2User.getAttributes());
        } else {
            return null;
        }

        String sub = oAuth2Response.getProviderId();
        String email = oAuth2Response.getEmail();
        String name = oAuth2Response.getName();
        String picture = oAuth2Response.getPicture();
        AuthRequest authRequest = new AuthRequest(sub, email, name, ROLE, picture);

        User existUser = userRepository.findBySub(sub);
        if (existUser == null) {
            User signUpUser = User.signUp(authRequest);
            userRepository.save(signUpUser);
            return new CustomOAuth2User(authRequest);
        } else {
            existUser.signIn(authRequest);
            userRepository.save(existUser);
            return new CustomOAuth2User(authRequest);
        }
    }
}
