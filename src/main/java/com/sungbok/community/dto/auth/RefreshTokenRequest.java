package com.sungbok.community.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Refresh Token 요청 DTO
 * POST /auth/refresh 엔드포인트에서 사용
 *
 * @since 0.0.1
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh Token은 필수입니다")
    private String refreshToken;
}
