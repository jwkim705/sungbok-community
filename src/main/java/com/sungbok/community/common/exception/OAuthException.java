package com.sungbok.community.common.exception;

import java.io.Serial;

/**
 * OAuth 소셜 로그인 예외
 * Google, Kakao, Naver OAuth 인증 실패
 *
 * @since 1.1.0
 */
public class OAuthException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ErrorCode와 원인 예외로 OAuth 예외 생성
     *
     * @param errorCode OAuth 에러 코드 (OAuthErrorCode)
     * @param cause 원인 예외
     */
    public OAuthException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, errorCode.getMessage(), cause);
    }
}
