package com.sungbok.community.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.io.Serial;
import java.util.Map;

/**
 * 모든 비즈니스 예외의 추상 베이스 클래스
 * ErrorCode Interface 기반으로 타입 안전성과 확장성 확보
 *
 * 2025 Best Practice: 매직 스트링 완전 제거, ErrorCode Enum 사용
 *
 * @since 1.1.0
 */
@Getter
public abstract class BaseException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 에러 코드 (도메인별 Enum)
     */
    private final ErrorCode errorCode;

    /**
     * 추가 상세 정보 (예: 리소스 ID, 필드 에러 등)
     */
    private final Map<String, Object> details;

    /**
     * ErrorCode만으로 예외 생성
     *
     * @param errorCode 에러 코드
     */
    protected BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = null;
    }

    /**
     * ErrorCode와 커스텀 메시지로 예외 생성
     *
     * @param errorCode 에러 코드
     * @param customMessage 커스텀 에러 메시지
     */
    protected BaseException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
        this.details = null;
    }

    /**
     * ErrorCode와 상세 정보로 예외 생성
     *
     * @param errorCode 에러 코드
     * @param details 추가 상세 정보 (Map)
     */
    protected BaseException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = details;
    }

    /**
     * ErrorCode, 커스텀 메시지, 원인 예외로 예외 생성
     *
     * @param errorCode 에러 코드
     * @param customMessage 커스텀 에러 메시지
     * @param cause 원인 예외
     */
    protected BaseException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.errorCode = errorCode;
        this.details = null;
    }

    /**
     * HTTP 상태 코드 반환
     *
     * @return HTTP 상태 (ErrorCode에서 자동 매핑)
     */
    public HttpStatus getHttpStatus() {
        return errorCode.getHttpStatus();
    }
}
