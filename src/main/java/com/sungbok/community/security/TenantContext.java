package com.sungbok.community.security;

import org.jspecify.annotations.Nullable;

/**
 * ThreadLocal 기반 테넌트 컨텍스트
 * 현재 요청의 app_id를 저장하고 조회
 *
 * WARNING: 반드시 요청 완료 후 clear() 호출 필요 (메모리 누수 방지)
 *
 * @since 0.0.1
 */
public class TenantContext {

    private static final ThreadLocal<Long> currentAppId = new ThreadLocal<>();

    /** 유틸리티 클래스는 인스턴스화 불가 */
    private TenantContext() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * 현재 스레드의 app_id 설정
     *
     * @param appId 테넌트 app ID
     * @throws IllegalArgumentException appId가 null이거나 0 이하인 경우
     */
    public static void setAppId(Long appId) {
        if (appId == null || appId <= 0) {
            throw new IllegalArgumentException("appId must be positive");
        }
        currentAppId.set(appId);
    }

    /**
     * 현재 스레드의 app_id 조회
     *
     * @return app_id (설정되지 않았으면 null)
     */
    public static @Nullable Long getAppId() {
        return currentAppId.get();
    }

    /**
     * 현재 스레드의 app_id 조회 (필수)
     * app_id가 설정되지 않은 경우 예외 발생
     *
     * @return app_id (null 아님)
     * @throws IllegalStateException app_id가 설정되지 않은 경우
     */
    public static Long getRequiredAppId() {
        Long appId = currentAppId.get();
        if (appId == null) {
            throw new IllegalStateException(
                "Tenant context not initialized. Ensure JWT filter has set appId."
            );
        }
        return appId;
    }

    /**
     * ThreadLocal 정리 (메모리 누수 방지)
     * 반드시 요청 완료 후 호출해야 함 (필터의 finally 블록)
     */
    public static void clear() {
        currentAppId.remove();
    }
}
