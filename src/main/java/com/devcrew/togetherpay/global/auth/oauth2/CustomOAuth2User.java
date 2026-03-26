package com.devcrew.togetherpay.global.auth.oauth2;

import com.devcrew.togetherpay.domain.user.UserRole;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
public class CustomOAuth2User implements OAuth2User {
    private final Long userId;
    private final UserRole role;
    private Map<String, Object> attributes;

    public CustomOAuth2User(Long userId, UserRole role, Map<String, Object> attributes) {
        this.userId = userId;
        this.role = role;
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.getKey()));
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}

