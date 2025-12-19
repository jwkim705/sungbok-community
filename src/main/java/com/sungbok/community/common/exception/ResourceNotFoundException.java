package com.sungbok.community.common.exception;

import java.io.Serial;
import java.util.Map;

/**
 * 리소스 미발견 예외
 * 사용자, 게시글, 조직 등 리소스를 찾을 수 없을 때
 *
 * @since 1.1.0
 */
public class ResourceNotFoundException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ErrorCode로 리소스 미발견 예외 생성
     *
     * @param errorCode 리소스 에러 코드 (ResourceErrorCode, TenantErrorCode 등)
     */
    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * ErrorCode와 상세 정보로 리소스 미발견 예외 생성
     *
     * @param errorCode 리소스 에러 코드
     * @param details 추가 정보 (예: orgId, userId 등)
     */
    public ResourceNotFoundException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }
}
