package com.sungbok.community.repository;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.generated.tables.daos.AuditLogsDao;
import org.jooq.generated.tables.pojos.AuditLogs;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static com.sungbok.community.repository.util.JooqTenantConditionUtils.orgIdCondition;
import static org.jooq.generated.Tables.AUDIT_LOGS;

/**
 * 감사 로그 데이터 접근 Repository
 * 보안 추적 및 변경 이력 관리
 * 하이브리드 DAO + DSL 패턴 사용
 *
 * 특징: org_id가 NULL이면 플랫폼 레벨 작업 (예: 플랫폼 관리자 작업)
 *       변경 불가 (INSERT only, created_at만 존재)
 */
@Repository
public class AuditLogsRepository {

    private final DSLContext dslContext;
    private final AuditLogsDao dao;

    public AuditLogsRepository(DSLContext dslContext, Configuration configuration) {
        this.dslContext = dslContext;
        this.dao = new AuditLogsDao(configuration);
    }

    /**
     * 감사 로그를 삽입합니다.
     * 비동기 호출용, 로깅 실패는 비즈니스 로직에 영향 없음
     *
     * @param auditLog 삽입할 감사 로그 엔티티
     * @return 삽입된 감사 로그 (모든 필드 포함)
     */
    public AuditLogs insert(AuditLogs auditLog) {
        // 현재 시간 설정 (created_at)
        if (auditLog.getCreatedAt() == null) {
            auditLog.setCreatedAt(LocalDateTime.now());
        }

        dao.insert(auditLog);  // DAO 패턴
        return auditLog;
    }

    /**
     * 조직의 감사 로그 조회 (페이징)
     * org_id 자동 필터링
     * 관리자 전용
     *
     * @param limit 조회할 개수
     * @param offset 건너뛸 개수
     * @return 감사 로그 리스트 (최신순)
     */
    public List<AuditLogs> fetchByOrgId(int limit, int offset) {
        return dslContext.selectFrom(AUDIT_LOGS)
                .where(orgIdCondition(AUDIT_LOGS.ORG_ID))
                .orderBy(AUDIT_LOGS.CREATED_AT.desc())
                .limit(limit)
                .offset(offset)
                .fetchInto(AuditLogs.class);
    }

    /**
     * 특정 사용자의 감사 로그 조회 (페이징)
     * org_id 자동 필터링
     *
     * @param userId 사용자 ID
     * @param limit 조회할 개수
     * @param offset 건너뛸 개수
     * @return 감사 로그 리스트 (최신순)
     */
    public List<AuditLogs> fetchByUserId(Long userId, int limit, int offset) {
        return dslContext.selectFrom(AUDIT_LOGS)
                .where(orgIdCondition(AUDIT_LOGS.ORG_ID))
                .and(AUDIT_LOGS.USER_ID.eq(userId))
                .orderBy(AUDIT_LOGS.CREATED_AT.desc())
                .limit(limit)
                .offset(offset)
                .fetchInto(AuditLogs.class);
    }

    /**
     * 특정 리소스의 변경 이력 조회
     * org_id 자동 필터링
     *
     * @param resourceType 리소스 타입 (예: post, membership, role)
     * @param resourceId 리소스 ID
     * @return 감사 로그 리스트 (최신순)
     */
    public List<AuditLogs> fetchByResource(String resourceType, Long resourceId) {
        return dslContext.selectFrom(AUDIT_LOGS)
                .where(orgIdCondition(AUDIT_LOGS.ORG_ID))
                .and(AUDIT_LOGS.RESOURCE_TYPE.eq(resourceType))
                .and(AUDIT_LOGS.RESOURCE_ID.eq(resourceId))
                .orderBy(AUDIT_LOGS.CREATED_AT.desc())
                .fetchInto(AuditLogs.class);
    }

    /**
     * 특정 작업 타입의 감사 로그 조회 (페이징)
     * org_id 자동 필터링
     *
     * @param action 작업 타입 (예: USER_LOGIN, POST_DELETE, ROLE_CHANGE)
     * @param limit 조회할 개수
     * @param offset 건너뛸 개수
     * @return 감사 로그 리스트 (최신순)
     */
    public List<AuditLogs> fetchByAction(String action, int limit, int offset) {
        return dslContext.selectFrom(AUDIT_LOGS)
                .where(orgIdCondition(AUDIT_LOGS.ORG_ID))
                .and(AUDIT_LOGS.ACTION.eq(action))
                .orderBy(AUDIT_LOGS.CREATED_AT.desc())
                .limit(limit)
                .offset(offset)
                .fetchInto(AuditLogs.class);
    }

    /**
     * 조직의 감사 로그 총 개수 조회
     * org_id 자동 필터링
     *
     * @return 감사 로그 총 개수
     */
    public int countByOrgId() {
        return dslContext.selectCount()
                .from(AUDIT_LOGS)
                .where(orgIdCondition(AUDIT_LOGS.ORG_ID))
                .fetchOneInto(Integer.class);
    }

    /**
     * 특정 기간 내 감사 로그 조회
     * org_id 자동 필터링
     * 관리자 전용 (통계/분석)
     *
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param limit 조회할 개수
     * @param offset 건너뛸 개수
     * @return 감사 로그 리스트 (최신순)
     */
    public List<AuditLogs> fetchByDateRange(
            LocalDateTime startDate,
            LocalDateTime endDate,
            int limit,
            int offset
    ) {
        return dslContext.selectFrom(AUDIT_LOGS)
                .where(orgIdCondition(AUDIT_LOGS.ORG_ID))
                .and(AUDIT_LOGS.CREATED_AT.between(startDate, endDate))
                .orderBy(AUDIT_LOGS.CREATED_AT.desc())
                .limit(limit)
                .offset(offset)
                .fetchInto(AuditLogs.class);
    }

    /**
     * 플랫폼 레벨 감사 로그 조회 (org_id NULL)
     * 플랫폼 관리자 전용
     *
     * @param limit 조회할 개수
     * @param offset 건너뛸 개수
     * @return 감사 로그 리스트 (최신순)
     */
    public List<AuditLogs> fetchPlatformLevelLogs(int limit, int offset) {
        return dslContext.selectFrom(AUDIT_LOGS)
                .where(AUDIT_LOGS.ORG_ID.isNull())
                .orderBy(AUDIT_LOGS.CREATED_AT.desc())
                .limit(limit)
                .offset(offset)
                .fetchInto(AuditLogs.class);
    }
}
