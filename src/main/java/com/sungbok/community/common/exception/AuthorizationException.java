package com.sungbok.community.common.exception;

import java.io.Serial;

/**
 * 인가 실패 예외
 * 접근 권한 부족, 역할 권한 미달 등
 *
 * @since 1.1.0
 */
public class AuthorizationException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ErrorCode로 인가 예외 생성
     *
     * @param errorCode 인가 에러 코드 (AuthErrorCode.ACCESS_DENIED 등)
     */
    public AuthorizationException(ErrorCode errorCode) {
        super(errorCode);
    }
}
