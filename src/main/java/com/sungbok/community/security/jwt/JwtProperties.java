package com.sungbok.community.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 설정 프로퍼티
 * ES256 (ECDSA P-256) 알고리즘용 EC 키 페어 경로 및 토큰 만료 시간 관리
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * EC Private Key 경로 (ES256 서명용)
     * 예: classpath:keys/jwt-private.pem
     */
    private String privateKey;

    /**
     * EC Public Key 경로 (ES256 검증용)
     * 예: classpath:keys/jwt-public.pem
     */
    private String publicKey;

    /**
     * Access Token 만료 시간 (밀리초)
     * 기본값: 900000ms = 15분
     */
    private long accessTokenExpiration = 900000;

    /**
     * Refresh Token 만료 시간 (밀리초)
     * 기본값: 604800000ms = 7일
     */
    private long refreshTokenExpiration = 604800000;

    /**
     * JWT 발급자 (issuer)
     * 기본값: sungbok-community
     */
    private String issuer = "sungbok-community";

    /**
     * Authorization 헤더의 토큰 접두사
     * 기본값: "Bearer "
     */
    private String tokenPrefix = "Bearer ";

    /**
     * Authorization 헤더 이름
     * 기본값: "Authorization"
     */
    private String headerName = "Authorization";
}
