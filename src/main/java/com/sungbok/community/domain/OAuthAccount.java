package com.sungbok.community.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OAuthAccount {

    private Long id;
    private Long userId; // Foreign key to the User entity
    private String provider; // OAuth provider name (e.g., "google", "kakao", "naver")
    private String providerUserId; // User ID provided by the OAuth provider

    public OAuthAccount(Long userId, String provider, String providerUserId) {
        this.userId = userId;
        this.provider = provider;
        this.providerUserId = providerUserId;
    }
}
