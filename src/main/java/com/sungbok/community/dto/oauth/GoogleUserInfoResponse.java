package com.sungbok.community.dto.oauth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Google OAuth2 사용자 정보 응답 DTO
 *
 * @since 0.0.1
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GoogleUserInfoResponse {
    private String sub;        // Google User ID
    private String email;
    private String name;
    private String picture;
}
