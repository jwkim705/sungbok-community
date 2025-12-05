package com.sungbok.community.repository.util;

import com.sungbok.community.security.TenantContext;
import org.jooq.Condition;
import org.jooq.Field;

/**
 * jOOQ 테넌트 필터링 유틸리티
 * app_id 조건을 자동으로 추가
 *
 * @since 0.0.1
 */
public class JooqTenantConditionUtils {

    private JooqTenantConditionUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * app_id 조건 생성 (ThreadLocal에서 자동 추출)
     *
     * @param appIdField 테이블의 app_id 컬럼
     * @return app_id = ? 조건
     * @throws IllegalStateException TenantContext에 app_id가 설정되지 않은 경우
     */
    public static Condition appIdCondition(Field<Long> appIdField) {
        Long appId = TenantContext.getRequiredAppId();
        return appIdField.eq(appId);
    }

    /**
     * app_id 조건 생성 (명시적 값 전달)
     *
     * @param appIdField 테이블의 app_id 컬럼
     * @param appId 테넌트 ID
     * @return app_id = ? 조건
     * @throws IllegalArgumentException appId가 null이거나 0 이하인 경우
     */
    public static Condition appIdCondition(Field<Long> appIdField, Long appId) {
        if (appId == null || appId <= 0) {
            throw new IllegalArgumentException("appId must be positive");
        }
        return appIdField.eq(appId);
    }

    /**
     * 기존 조건에 app_id 조건 추가
     *
     * @param condition 기존 조건
     * @param appIdField 테이블의 app_id 컬럼
     * @return 기존 조건 AND app_id = ?
     */
    public static Condition withAppId(Condition condition, Field<Long> appIdField) {
        return condition.and(appIdCondition(appIdField));
    }
}
