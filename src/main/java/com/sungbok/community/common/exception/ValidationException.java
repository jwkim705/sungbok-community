package com.sungbok.community.common.exception;

import java.io.Serial;
import java.util.Map;

/**
 * 유효성 검증 실패 예외
 * 입력 값 검증 실패, 형식 오류 등
 *
 * @since 1.1.0
 */
public class ValidationException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ErrorCode와 필드별 에러로 검증 예외 생성
     *
     * @param errorCode 검증 에러 코드 (ValidationErrorCode)
     * @param fieldErrors 필드별 에러 맵 (key: 필드명, value: 에러 메시지)
     */
    public ValidationException(ErrorCode errorCode, Map<String, Object> fieldErrors) {
        super(errorCode, fieldErrors);
    }
}
