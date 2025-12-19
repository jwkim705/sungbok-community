package com.sungbok.community.common.exception;

import java.io.Serial;

/**
 * 시스템 에러 예외
 * 파일 업로드 실패, 내부 서버 오류 등 시스템 관련 예외
 *
 * @since 1.1.0
 */
public class SystemException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ErrorCode만으로 예외 생성
     *
     * @param errorCode 에러 코드
     */
    public SystemException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * ErrorCode와 원인 예외로 예외 생성
     *
     * @param errorCode 에러 코드
     * @param cause 원인 예외
     */
    public SystemException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, errorCode.getMessage(), cause);
    }
}
