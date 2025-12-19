package com.sungbok.community.common.exception;

import java.io.Serial;

/**
 * 인증 실패 예외
 * 인증 토큰 무효/만료, 로그인 실패 등
 *
 * @since 1.1.0
 */
public class AuthenticationException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ErrorCode로 인증 예외 생성
     *
     * @param errorCode 인증 에러 코드 (AuthErrorCode)
     */
    public AuthenticationException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * ErrorCode와 원인 예외로 인증 예외 생성
     *
     * @param errorCode 인증 에러 코드 (AuthErrorCode)
     * @param cause 원인 예외
     */
    public AuthenticationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, errorCode.getMessage(), cause);
    }
}
