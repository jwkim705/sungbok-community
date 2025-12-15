package com.sungbok.community.security;

import org.jspecify.annotations.Nullable;

/**
 * ThreadLocal 기반 테넌트 컨텍스트
 * 현재 요청의 org_id를 저장하고 조회
 *
 * WARNING: 반드시 요청 완료 후 clear() 호출 필요 (메모리 누수 방지)
 *
 * @since 0.0.1
 */
public class TenantContext {

    private static final ThreadLocal<Long> currentOrgId = new ThreadLocal<>();

    /** 유틸리티 클래스는 인스턴스화 불가 */
    private TenantContext() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * 현재 스레드의 org_id 설정
     *
     * @param orgId 테넌트 organization ID
     * @throws IllegalArgumentException orgId가 null이거나 0 이하인 경우
     */
    public static void setOrgId(Long orgId) {
        if (orgId == null || orgId <= 0) {
            throw new IllegalArgumentException("orgId must be positive");
        }
        currentOrgId.set(orgId);
    }

    /**
     * 현재 스레드의 org_id 조회
     *
     * @return org_id (설정되지 않았으면 null)
     */
    public static @Nullable Long getOrgId() {
        return currentOrgId.get();
    }

    /**
     * 현재 스레드의 org_id 조회 (필수)
     * org_id가 설정되지 않은 경우 예외 발생
     *
     * @return org_id (null 아님)
     * @throws IllegalStateException org_id가 설정되지 않은 경우
     */
    public static Long getRequiredOrgId() {
        Long orgId = currentOrgId.get();
        if (orgId == null) {
            throw new IllegalStateException(
                "Tenant context not initialized. Ensure JWT filter has set orgId."
            );
        }
        return orgId;
    }

    /**
     * ThreadLocal 정리 (메모리 누수 방지)
     * 반드시 요청 완료 후 호출해야 함 (필터의 finally 블록)
     */
    public static void clear() {
        currentOrgId.remove();
    }
}
