package com.sungbok.community.common.exception.code;

import com.sungbok.community.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 유효성 검증 관련 에러 코드
 * 입력 값 검증 실패 (400) 에러 정의
 *
 * @since 1.1.0
 */
@Getter
@RequiredArgsConstructor
public enum ValidationErrorCode implements ErrorCode {

    FAILED("VAL_001", HttpStatus.BAD_REQUEST, "입력 값 검증에 실패했습니다"),
    INVALID_FORMAT("VAL_002", HttpStatus.BAD_REQUEST, "입력 형식이 올바르지 않습니다"),
    FILE_TOO_LARGE("VAL_003", HttpStatus.BAD_REQUEST, "파일 크기가 제한을 초과했습니다"),
    INVALID_FILE_TYPE("VAL_004", HttpStatus.BAD_REQUEST, "허용되지 않는 파일 형식입니다"),
    INVALID_FILE_NAME("VAL_005", HttpStatus.BAD_REQUEST, "잘못된 파일 이름입니다");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}
