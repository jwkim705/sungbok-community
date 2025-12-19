package com.sungbok.community.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * OAuth2 소셜 로그인을 위한 설정
 * RestClient Bean 등록 및 타임아웃 설정
 * HTTP 엔진: JDK HttpClient (Java 11+)
 */
@Configuration
public class OAuth2Config {

    /**
     * OAuth2 API 호출용 RestClient Bean 등록
     * Spring Boot 4.0의 권장 방식 (RestTemplate 대체)
     * HTTP 엔진으로 JDK HttpClient 사용
     *
     * @return 설정된 RestClient 인스턴스
     */
    @Bean
    public RestClient restClient() {
        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        return RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
    }
}
