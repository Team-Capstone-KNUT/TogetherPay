package com.devcrew.togetherpay.global.auth.jwt;

import com.devcrew.togetherpay.domain.user.UserRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class TokenProvider {
    private final String secret;
    private final long tokenValidityInMilliseconds;
    private SecretKey key;

    public TokenProvider(
            @Value("${jwt.client-secret}") String secret, //
            @Value("${jwt.expiry-seconds}") long tokenValidityInSeconds) { //
            this.secret = secret;
            this.tokenValidityInMilliseconds = tokenValidityInSeconds * 1000;
    }

    @PostConstruct
    public void init(){
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }
    // 토큰 생성
    public String createToken(Long userId, UserRole role) {
        return buildToken(userId, tokenValidityInMilliseconds, role.name());
    }

    // 리프레시 토큰 생성
    public String createRefreshToken(Long userId) {
        long refreshValidity = 1000L * 60 * 60 * 24 * 14;
        return buildToken(userId, refreshValidity, null);
    }

    // 토큰 생성시 생성 메서드
    private String buildToken(Long userId, long validityMilliseconds, String role) {
        long now = (new Date()).getTime(); // 현재 시간 가져와서
        Date validity = new Date(now + validityMilliseconds);

        JwtBuilder builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .expiration(validity)
                .signWith(key);

        // 리프레시 토큰에서는 role이 필요 없음.
        if (role != null) {
            builder.claim("role", role);
        }
        return builder.compact();
    }

    public Authentication getAuthentication(String token) {
        Long userId = getUserId(token);
        String role = getRole(token);

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
        return new UsernamePasswordAuthenticationToken(userId, token, authorities);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.info("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.info("잘못된 JWT 토큰입니다.");
        }
        return false;
    }

    public Long getUserId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    private String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    // 액세스 토큰 유효시간 계산 메서드
    public Long getExpiration(String accessToken) {
        Date expiration = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(accessToken)
                .getPayload()
                .getExpiration();
        return expiration.getTime();
    }
}
