package com.devcrew.togetherpay.global.auth.oauth2;

import com.devcrew.togetherpay.domain.user.User;
import com.devcrew.togetherpay.domain.user.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class OAuthAttributes {
    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String nickname;
    private String email;
    private String provider;
    private String providerId;

    // 소셜 로그인 제공자(google, kakao)에 따라서 팩토리 메서드로 선언해둠
    public static OAuthAttributes of(String registrationId,
                                     String userNameAttributeName,
                                     Map<String, Object> attributes) {
        // 이 부분. 지금은 두가지인데 세개면 elseif만 추가하면 되긴하는데 좀더 효율적인 방법은?
        if ("kakao".equals(registrationId)) {
            return ofKakao("id", attributes); // 카카오용 번역
        }
        return ofGoogle(userNameAttributeName, attributes); // 기본은 구글 번역
    }

    // 구글 데이터용 번역
    private static OAuthAttributes ofGoogle(String userNameAttributeName,
                                            Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .nickname((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .provider("google")
                .providerId(String.valueOf(attributes.get("sub")))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    // 카카오 데이터용 번역(카카오 경우에 Json 구조가 좀 깊음)
    private static OAuthAttributes ofKakao(String userNameAttributeName,
                                            Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        return OAuthAttributes.builder()
                .nickname((String) profile.get("nickname"))
                .email((String) kakaoAccount.get("email"))
                .provider("kakao")
                .providerId(String.valueOf(attributes.get("id")))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    // 신규 유저는 DB에 넣을 수 있도록 User 엔티티로 변환하는 메서드 정의
    public User toEntity() {
        return User.builder()
                .nickname(nickname)
                .email(email)
                .role(UserRole.USER) // 가입 기본 권한을 USER로 설정.
                .provider(provider)
                .providerId(providerId)
                .build();
    }
}
