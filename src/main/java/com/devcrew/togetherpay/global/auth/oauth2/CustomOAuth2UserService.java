package com.devcrew.togetherpay.global.auth.oauth2;

import com.devcrew.togetherpay.domain.user.User;
import com.devcrew.togetherpay.domain.user.repository.UserRespository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final UserRespository userRespository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // DefaultOAuth2UserService를
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);
        // 구글인지 카카오인지 확인
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        // 소셜 서버에서 유저 식별하는 고유 키값의 이름(구글은 "sub", 카카오는 "id")
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();
        // 구글 및 카카오 데이터를 OAuthAttributes의 of 메서드를 통해서 정한 포맷에 맞춘다.
        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());
        User user = saveOrUpdate(attributes);

        return new CustomOAuth2User(
                user.getId(),
                user.getRole(),
                attributes.getAttributes()
        );
    }

    private User saveOrUpdate(OAuthAttributes attributes) {
        User user = userRespository.findByEmail(attributes.getEmail())
                .map(entity -> {
                    entity.updateNickname(attributes.getNickname());
                    return entity;
                })
                .orElse(attributes.toEntity());
        return userRespository.save(user);
    }

}
