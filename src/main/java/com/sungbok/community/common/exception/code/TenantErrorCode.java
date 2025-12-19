package com.sungbok.community.common.exception.code;

import com.sungbok.community.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 멀티테넌시 관련 에러 코드
 * 조직(Organization) 접근 관련 에러 정의
 *
 * @since 1.1.0
 */
@Getter
@RequiredArgsConstructor
public enum TenantErrorCode implements ErrorCode {

    NOT_FOUND("TEN_001", HttpStatus.NOT_FOUND, "조직을 찾을 수 없습니다"),
    ACCESS_DENIED("TEN_002", HttpStatus.FORBIDDEN, "해당 조직에 접근할 수 없습니다");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}
