package com.sungbok.community.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * JWT 토큰 응답 DTO
 * 로그인 및 토큰 갱신 시 반환
 *
 * @since 0.0.1
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 8234567890123456789L;

    /**
     * Access Token (15분)
     */
    private String accessToken;

    /**
     * Refresh Token (7일)
     */
    private String refreshToken;

    /**
     * 토큰 타입 (기본: "Bearer")
     */
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * Access Token 만료 시간 (초 단위)
     * 예: 900 = 15분
     */
    private long expiresIn;
}
