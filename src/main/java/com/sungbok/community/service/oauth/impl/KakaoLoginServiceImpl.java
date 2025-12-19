package com.sungbok.community.service.oauth.impl;

import com.sungbok.community.common.exception.OAuthException;
import com.sungbok.community.common.exception.code.OAuthErrorCode;
import com.sungbok.community.dto.SocialUserInfo;
import com.sungbok.community.dto.oauth.KakaoTokenResponse;
import com.sungbok.community.dto.oauth.KakaoUserInfoResponse;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoLoginServiceImpl implements SocialLoginService {

    private final RestClient restClient;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String redirectUri;

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    @Override
    public SocialType getServiceName() {
        return SocialType.KAKAO;
    }

    @Override
    @Retryable(
        includes = {RestClientException.class, ResourceAccessException.class},
        delay = 1000,
        multiplier = 2.0,
        maxDelay = 5000
    )
    public String getSocialAccessToken(String code, String codeVerifier) {
        // Kakao는 PKCE를 지원하지 않으므로 codeVerifier는 무시됨
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);

        try {
            KakaoTokenResponse response = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(params)
                    .retrieve()
                    .body(KakaoTokenResponse.class);

            log.info("Kakao 토큰 교환 성공");
            return response.getAccessToken();
        } catch (HttpClientErrorException e) {
            // 4xx 에러: 잘못된 요청 (invalid code, expired code 등)
            log.error("Kakao 토큰 교환 실패 (클라이언트 에러): status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new OAuthException(OAuthErrorCode.INVALID_CODE, e);
        } catch (HttpServerErrorException e) {
            // 5xx 에러: Kakao 서버 에러
            log.error("Kakao 토큰 교환 실패 (서버 에러): status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new OAuthException(OAuthErrorCode.PROVIDER_ERROR, e);
        } catch (RestClientException e) {
            // 네트워크 에러 또는 기타 RestClient 에러 (ResourceAccessException 포함)
            log.error("Kakao 토큰 교환 실패 (네트워크 에러)", e);
            throw new OAuthException(OAuthErrorCode.PROVIDER_ERROR, e);
        } catch (Exception e) {
            // 예상치 못한 에러
            log.error("Kakao 토큰 교환 실패 (예상치 못한 에러)", e);
            throw new OAuthException(OAuthErrorCode.PROVIDER_ERROR, e);
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
            KakaoUserInfoResponse userInfo = restClient.get()
                    .uri(USER_INFO_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(KakaoUserInfoResponse.class);

            String email = userInfo.getKakaoAccount() != null ? userInfo.getKakaoAccount().getEmail() : null;
            String name = null;
            String picture = null;

            if (userInfo.getKakaoAccount() != null && userInfo.getKakaoAccount().getProfile() != null) {
                name = userInfo.getKakaoAccount().getProfile().getNickname();
                picture = userInfo.getKakaoAccount().getProfile().getProfileImageUrl();
            }

            log.info("Kakao 사용자 정보 조회 성공: email={}", email);

            return SocialUserInfo.builder()
                .socialType(SocialType.KAKAO)
                .providerId(userInfo.getId())
                .email(email)
                .name(name)
                .picture(picture)
                .build();
        } catch (HttpClientErrorException e) {
            // 4xx 에러: 잘못된 Access Token (만료, 권한 부족 등)
            log.error("Kakao 사용자 정보 조회 실패 (클라이언트 에러): status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new OAuthException(OAuthErrorCode.INVALID_CODE, e);
        } catch (HttpServerErrorException e) {
            // 5xx 에러: Kakao 서버 에러
            log.error("Kakao 사용자 정보 조회 실패 (서버 에러): status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new OAuthException(OAuthErrorCode.PROVIDER_ERROR, e);
        } catch (RestClientException e) {
            // 네트워크 에러 또는 기타 RestClient 에러 (ResourceAccessException 포함)
            log.error("Kakao 사용자 정보 조회 실패 (네트워크 에러)", e);
            throw new OAuthException(OAuthErrorCode.PROVIDER_ERROR, e);
        } catch (Exception e) {
            // 예상치 못한 에러
            log.error("Kakao 사용자 정보 조회 실패 (예상치 못한 에러)", e);
            throw new OAuthException(OAuthErrorCode.PROVIDER_ERROR, e);
        }
    }
}
