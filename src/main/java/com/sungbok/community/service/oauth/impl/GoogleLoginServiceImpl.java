package com.sungbok.community.service.oauth.impl;

import com.sungbok.community.dto.SocialUserInfo;
import com.sungbok.community.dto.oauth.GoogleTokenResponse;
import com.sungbok.community.dto.oauth.GoogleUserInfoResponse;
import com.sungbok.community.enums.SocialType;
import com.sungbok.community.service.oauth.SocialLoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleLoginServiceImpl implements SocialLoginService {

    private final RestClient restClient;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v1/userinfo";

    @Override
    public SocialType getServiceName() {
        return SocialType.GOOGLE;
    }

    @Override
    @Retryable(
        includes = {RestClientException.class, ResourceAccessException.class},
        delay = 1000,
        multiplier = 2.0,
        maxDelay = 5000
    )
    public String getSocialAccessToken(String code, String codeVerifier) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);

        if (codeVerifier != null && !codeVerifier.isBlank()) {
            params.add("code_verifier", codeVerifier);
        }

        try {
            GoogleTokenResponse response = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(params)
                    .retrieve()
                    .body(GoogleTokenResponse.class);

            log.info("Google 토큰 교환 성공");
            return response.getAccessToken();
        } catch (Exception e) {
            log.error("Google 토큰 교환 실패", e);
            throw new RuntimeException("Failed to exchange Google authorization code", e);
        }
    }

    @Override
    @Retryable(
        includes = {RestClientException.class, ResourceAccessException.class},
        delay = 1000,
        multiplier = 2.0,
        maxDelay = 5000
    )
    public SocialUserInfo getSocialUserInfo(String accessToken) {
        try {
            GoogleUserInfoResponse userInfo = restClient.get()
                    .uri(USER_INFO_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(GoogleUserInfoResponse.class);

            log.info("Google 사용자 정보 조회 성공: email={}", userInfo.getEmail());

            return SocialUserInfo.builder()
                .socialType(SocialType.GOOGLE)
                .providerId(userInfo.getSub())
                .email(userInfo.getEmail())
                .name(userInfo.getName())
                .picture(userInfo.getPicture())
                .build();
        } catch (Exception e) {
            log.error("Google 사용자 정보 조회 실패", e);
            throw new RuntimeException("Failed to get Google user info", e);
        }
    }
}
