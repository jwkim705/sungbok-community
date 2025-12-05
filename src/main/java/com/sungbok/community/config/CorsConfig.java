package com.sungbok.community.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CORS 설정
 * 환경별로 허용할 도메인을 명시적으로 지정하여 보안 강화
 *
 * @since 0.0.1
 */
@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "app.cors")
public class CorsConfig {

    // Getter and Setter (required for @ConfigurationProperties)
    private List<String> allowedOrigins = new ArrayList<>();

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // 환경별 구체적인 도메인 지정 (보안 강화)
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(
                Arrays.asList("HEAD", "GET", "POST", "PUT", "DELETE", "PATCH")
        );
        configuration.setAllowedHeaders(
                Arrays.asList("Authorization", "Cache-Control", "Content-Type", "X-Session-Id")
        );
        configuration.setExposedHeaders(
            List.of("X-Session-Id") // 모바일 앱이 응답 헤더의 세션 ID를 읽을 수 있도록
        );
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // preflight 캐싱 (1시간)

        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
