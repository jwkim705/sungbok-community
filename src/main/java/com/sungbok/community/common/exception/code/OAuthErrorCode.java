package com.sungbok.community.common.exception.code;

import com.sungbok.community.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * OAuth 소셜 로그인 관련 에러 코드
 * Google, Kakao, Naver OAuth 에러 정의
 *
 * @since 1.1.0
 */
@Getter
@RequiredArgsConstructor
public enum OAuthErrorCode implements ErrorCode {

    PROVIDER_ERROR("OAUTH_001", HttpStatus.BAD_GATEWAY, "OAuth 인증 서버 오류"),
    INVALID_CODE("OAUTH_002", HttpStatus.BAD_REQUEST, "OAuth 인증 코드가 유효하지 않습니다"),
    TOKEN_REQUEST_FAILED("OAUTH_003", HttpStatus.BAD_GATEWAY, "OAuth 토큰 요청 실패");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}
