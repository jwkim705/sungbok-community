package com.sungbok.community.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 에러 코드 공통 인터페이스
 * 도메인별 Enum이 구현하여 확장성과 타입 안전성을 동시에 확보
 *
 * 2025 Best Practice: Interface 기반 도메인 분리로 매직 스트링 완전 제거
 *
 * @since 1.1.0
 */
public interface ErrorCode {

    /**
     * 프론트엔드 분기용 짧은 코드
     *
     * @return 에러 코드 (예: "AUTH_001", "RES_001")
     */
    String getCode();

    /**
     * Enum 이름
     *
     * @return Enum 상수 이름 (예: "INVALID_TOKEN", "NOT_FOUND")
     */
    String name();

    /**
     * HTTP 상태 코드
     *
     * @return HTTP 상태 (예: 401, 404, 500)
     */
    HttpStatus getHttpStatus();

    /**
     * 기본 에러 메시지 (한국어)
     *
     * @return 에러 메시지 (예: "인증 토큰이 유효하지 않습니다")
     */
    String getMessage();
}
