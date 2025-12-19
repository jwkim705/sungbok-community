package com.sungbok.community.common.exception.code;

import com.sungbok.community.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 인증/인가 관련 에러 코드
 * Authentication (401) 및 Authorization (403) 에러 정의
 *
 * @since 1.1.0
 */
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    // Authentication (401)
    INVALID_TOKEN("AUTH_001", HttpStatus.UNAUTHORIZED, "인증 토큰이 유효하지 않습니다"),
    EXPIRED_TOKEN("AUTH_002", HttpStatus.UNAUTHORIZED, "인증 토큰이 만료되었습니다"),
    TOKEN_NOT_FOUND("AUTH_003", HttpStatus.UNAUTHORIZED, "인증 토큰을 찾을 수 없습니다"),
    INVALID_CREDENTIALS("AUTH_004", HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다"),
    TOKEN_MISMATCH("AUTH_005", HttpStatus.BAD_REQUEST, "토큰이 일치하지 않거나 이미 로그아웃되었습니다"),

    // Authorization (403)
    ACCESS_DENIED("AUTHZ_001", HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),
    INSUFFICIENT_ROLE("AUTHZ_002", HttpStatus.FORBIDDEN, "권한이 부족합니다");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}
