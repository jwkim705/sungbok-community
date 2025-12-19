package com.sungbok.community.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * 로그아웃 요청 DTO
 * DELIFEQ 명령어로 안전한 토큰 삭제를 위한 Refresh Token 전달
 *
 * @since 0.0.1
 */
public record LogoutRequest(
    @NotBlank(message = "Refresh token is required")
    String refreshToken
) {}
