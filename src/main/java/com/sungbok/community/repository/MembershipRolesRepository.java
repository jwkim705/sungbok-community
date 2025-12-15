package com.sungbok.community.repository;

import com.sungbok.community.security.TenantContext;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.generated.tables.daos.MembershipRolesDao;
import org.jooq.generated.tables.pojos.MembershipRoles;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.jooq.generated.Tables.MEMBERSHIP_ROLES;
import static com.sungbok.community.repository.util.JooqTenantConditionUtils.orgIdCondition;

/**
 * 멤버십-역할 N:M 관계 Repository
 * 하이브리드 DAO + DSL 패턴 사용
 */
@Repository
public class MembershipRolesRepository {

    private final DSLContext dsl;
    private final MembershipRolesDao dao;

    public MembershipRolesRepository(DSLContext dsl, Configuration configuration) {
        this.dsl = dsl;
        this.dao = new MembershipRolesDao(configuration);
    }

    /**
     * 멤버십 ID로 역할 목록 조회
     *
     * @param membershipId 멤버십 ID
     * @return 역할 목록
     */
    public List<MembershipRoles> fetchByMembershipId(Long membershipId) {
        return dsl.selectFrom(MEMBERSHIP_ROLES)
                .where(orgIdCondition(MEMBERSHIP_ROLES.ORG_ID))
                .and(MEMBERSHIP_ROLES.MEMBERSHIP_ID.eq(membershipId))
                .orderBy(MEMBERSHIP_ROLES.IS_PRIMARY.desc(), MEMBERSHIP_ROLES.ASSIGNED_AT.asc())
                .fetchInto(MembershipRoles.class);
    }

    /**
     * 멤버십의 역할 ID 목록만 조회 (JWT 생성용)
     *
     * @param membershipId 멤버십 ID
     * @return 역할 ID 목록
     */
    public List<Long> fetchRoleIdsByMembershipId(Long membershipId) {
        return dsl.select(MEMBERSHIP_ROLES.ROLE_ID)
                .from(MEMBERSHIP_ROLES)
                .where(orgIdCondition(MEMBERSHIP_ROLES.ORG_ID))
                .and(MEMBERSHIP_ROLES.MEMBERSHIP_ID.eq(membershipId))
                .orderBy(MEMBERSHIP_ROLES.IS_PRIMARY.desc())
                .fetchInto(Long.class);
    }

    /**
     * 역할 할당 (INSERT)
     *
     * @param membershipId 멤버십 ID
     * @param roleId 역할 ID
     * @param isPrimary 대표 역할 여부
     * @param assignedBy 할당자 ID
     * @return 생성된 MembershipRoles
     */
    public MembershipRoles assignRole(Long membershipId, Long roleId, Boolean isPrimary, Long assignedBy) {
        Long orgId = TenantContext.getRequiredOrgId();

        MembershipRoles record = new MembershipRoles();
        record.setOrgId(orgId);
        record.setMembershipId(membershipId);
        record.setRoleId(roleId);
        record.setIsPrimary(isPrimary);
        record.setAssignedBy(assignedBy);

        dao.insert(record);  // DAO 패턴
        return record;
    }

    /**
     * 특정 역할 제거 (겸직 지원)
     *
     * @param membershipId 멤버십 ID
     * @param roleId 제거할 역할 ID
     * @return 삭제된 행 수
     */
    public int removeRole(Long membershipId, Long roleId) {
        return dsl.deleteFrom(MEMBERSHIP_ROLES)
                .where(orgIdCondition(MEMBERSHIP_ROLES.ORG_ID))
                .and(MEMBERSHIP_ROLES.MEMBERSHIP_ID.eq(membershipId))
                .and(MEMBERSHIP_ROLES.ROLE_ID.eq(roleId))
                .execute();
    }

    /**
     * 멤버십의 모든 역할 제거
     *
     * @param membershipId 멤버십 ID
     * @return 삭제된 행 수
     */
    public int deleteAllRoles(Long membershipId) {
        return dsl.deleteFrom(MEMBERSHIP_ROLES)
                .where(orgIdCondition(MEMBERSHIP_ROLES.ORG_ID))
                .and(MEMBERSHIP_ROLES.MEMBERSHIP_ID.eq(membershipId))
                .execute();
    }

    /**
     * 역할 추가 (겸직 지원)
     * 기존 assignRole() 메서드 활용
     *
     * @param membershipId 멤버십 ID
     * @param roleId 추가할 역할 ID
     * @param isPrimary 주 역할 여부
     * @param assignedBy 할당자 ID
     * @return 생성된 MembershipRoles
     */
    public MembershipRoles addRole(Long membershipId, Long roleId, boolean isPrimary, Long assignedBy) {
        // 이미 존재하는 역할인지 확인
        boolean exists = dsl.fetchExists(
            dsl.selectOne()
                .from(MEMBERSHIP_ROLES)
                .where(MEMBERSHIP_ROLES.MEMBERSHIP_ID.eq(membershipId))
                .and(MEMBERSHIP_ROLES.ROLE_ID.eq(roleId))
                .and(orgIdCondition(MEMBERSHIP_ROLES.ORG_ID))
        );

        if (exists) {
            throw new IllegalStateException("Role already assigned to this membership");
        }

        // isPrimary=true면 다른 역할의 isPrimary를 false로 변경
        if (isPrimary) {
            dsl.update(MEMBERSHIP_ROLES)
                .set(MEMBERSHIP_ROLES.IS_PRIMARY, false)
                .where(MEMBERSHIP_ROLES.MEMBERSHIP_ID.eq(membershipId))
                .and(orgIdCondition(MEMBERSHIP_ROLES.ORG_ID))
                .execute();
        }

        return assignRole(membershipId, roleId, isPrimary, assignedBy);
    }

    /**
     * 주 역할 변경 (is_primary 플래그 업데이트)
     *
     * @param membershipId 멤버십 ID
     * @param roleId 주 역할로 설정할 역할 ID
     * @return 영향받은 행 수
     */
    public int setPrimaryRole(Long membershipId, Long roleId) {
        // 1. 모든 역할의 isPrimary를 false로 변경
        dsl.update(MEMBERSHIP_ROLES)
            .set(MEMBERSHIP_ROLES.IS_PRIMARY, false)
            .where(MEMBERSHIP_ROLES.MEMBERSHIP_ID.eq(membershipId))
            .and(orgIdCondition(MEMBERSHIP_ROLES.ORG_ID))
            .execute();

        // 2. 지정된 역할만 isPrimary=true로 변경
        return dsl.update(MEMBERSHIP_ROLES)
            .set(MEMBERSHIP_ROLES.IS_PRIMARY, true)
            .where(MEMBERSHIP_ROLES.MEMBERSHIP_ID.eq(membershipId))
            .and(MEMBERSHIP_ROLES.ROLE_ID.eq(roleId))
            .and(orgIdCondition(MEMBERSHIP_ROLES.ORG_ID))
            .execute();
    }

    /**
     * 기본 역할 할당 (멤버십 승인 시 자동 호출)
     *
     * @param membershipId 멤버십 ID
     * @param orgId 조직 ID
     * @param approvedBy 승인자 ID
     * @return 생성된 MembershipRoles
     */
    public MembershipRoles assignDefaultRole(Long membershipId, Long orgId, Long approvedBy) {
        // level=1인 기본 역할 조회 (성도)
        Long defaultRoleId = dsl.select(org.jooq.generated.Tables.ROLES.ID)
            .from(org.jooq.generated.Tables.ROLES)
            .where(org.jooq.generated.Tables.ROLES.ORG_ID.eq(orgId))
            .and(org.jooq.generated.Tables.ROLES.LEVEL.eq(1))
            .orderBy(org.jooq.generated.Tables.ROLES.ID.asc())
            .limit(1)
            .fetchOne(org.jooq.generated.Tables.ROLES.ID);

        if (defaultRoleId == null) {
            throw new IllegalStateException(
                String.format("No default role (level=1) found for organization %d", orgId)
            );
        }

        // 첫 번째 역할이므로 isPrimary=true
        return assignRole(membershipId, defaultRoleId, true, approvedBy);
    }
}
