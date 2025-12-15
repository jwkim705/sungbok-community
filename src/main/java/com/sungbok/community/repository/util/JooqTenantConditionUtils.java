package com.sungbok.community.repository.util;

import com.sungbok.community.security.TenantContext;
import org.jooq.Condition;
import org.jooq.Field;

/**
 * jOOQ 테넌트 필터링 유틸리티
 * org_id 조건을 자동으로 추가
 *
 * @since 0.0.1
 */
public class JooqTenantConditionUtils {

    private JooqTenantConditionUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * org_id 조건 생성 (ThreadLocal에서 자동 추출)
     *
     * @param orgIdField 테이블의 org_id 컬럼
     * @return org_id = ? 조건
     * @throws IllegalStateException TenantContext에 org_id가 설정되지 않은 경우
     */
    public static Condition orgIdCondition(Field<Long> orgIdField) {
        Long orgId = TenantContext.getRequiredOrgId();
        return orgIdField.eq(orgId);
    }

    /**
     * org_id 조건 생성 (명시적 값 전달)
     *
     * @param orgIdField 테이블의 org_id 컬럼
     * @param orgId 테넌트 ID
     * @return org_id = ? 조건
     * @throws IllegalArgumentException orgId가 null이거나 0 이하인 경우
     */
    public static Condition orgIdCondition(Field<Long> orgIdField, Long orgId) {
        if (orgId == null || orgId <= 0) {
            throw new IllegalArgumentException("orgId must be positive");
        }
        return orgIdField.eq(orgId);
    }

    /**
     * 기존 조건에 org_id 조건 추가
     *
     * @param condition 기존 조건
     * @param orgIdField 테이블의 org_id 컬럼
     * @return 기존 조건 AND org_id = ?
     */
    public static Condition withOrgId(Condition condition, Field<Long> orgIdField) {
        return condition.and(orgIdCondition(orgIdField));
    }
}
