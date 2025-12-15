package com.sungbok.community.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * OAuth2 Authorization Code 요청 DTO (OAuth 2.1)
 * 앱이 OAuth2 공급자로부터 받은 Authorization Code를 백엔드에 전달
 *
 * @since 0.0.1
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2CodeRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 8234567890123456794L;

    /**
     * OAuth2 Authorization Code
     * OAuth2 공급자(Google, Kakao, Naver)가 앱에 전달한 임시 코드
     */
    @NotBlank(message = "Authorization code는 필수입니다")
    private String code;

    /**
     * PKCE code_verifier (선택 사항)
     * OAuth2 공급자가 PKCE를 지원하는 경우 사용
     */
    private String codeVerifier;
}
