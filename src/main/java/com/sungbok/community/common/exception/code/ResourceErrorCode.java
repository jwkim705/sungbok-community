package com.sungbok.community.common.exception.code;

import com.sungbok.community.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 리소스 관련 에러 코드
 * 리소스 미발견 (404), 중복 (409) 에러 정의
 *
 * @since 1.1.0
 */
@Getter
@RequiredArgsConstructor
public enum ResourceErrorCode implements ErrorCode {

    NOT_FOUND("RES_001", HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다"),
    ALREADY_EXISTS("RES_002", HttpStatus.CONFLICT, "이미 존재하는 리소스입니다");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}
