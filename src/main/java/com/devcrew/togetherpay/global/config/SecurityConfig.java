package com.devcrew.togetherpay.global.config;

import com.devcrew.togetherpay.global.auth.jwt.JwtAuthenticationFilter;
import com.devcrew.togetherpay.global.auth.oauth2.CustomOAuth2UserService;
import com.devcrew.togetherpay.global.auth.oauth2.OAuth2LoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .formLogin(AbstractHttpConfigurer::disable) // 기본 폼 로그인 비활성화
                .httpBasic(AbstractHttpConfigurer::disable) // 기본 인증 비활성화
                .csrf(AbstractHttpConfigurer::disable) // csrf 방어 비활성화

                // [CORS 설정] 프론트에서 오는 요청(예) localhost:3000)을 허락한다.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // JWT 사용하니까 세션 만들지 못하게 설정
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // [권한 룰 설정] 접근 경로 주소 지정
                .authorizeHttpRequests(auth -> auth
                        // 💡 주의: /api/v1/user/me 같은 내 정보 조회는 여기에 넣으면 안 됩니다! (인증 필수)
                        .requestMatchers("/", "/login/**", "/oauth2/**", "/api/v1/auth/**").permitAll() // 여기는 프리패스
                        .anyRequest().authenticated() // 위 외에 접근 시 무조건 인증(토큰) 필요함.
                )

                // 소셜 로그인 세팅, customOAuth2UserService, successHandler 설정해줌
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2LoginSuccessHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")); // 허용할 HTTP 메서드
        config.setAllowedHeaders(List.of("*")); // 모든 헤더 허용
        config.setExposedHeaders(List.of("Authorization")); // 프론트에서 Authorization 헤더를 읽을 수 있게 허락

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
