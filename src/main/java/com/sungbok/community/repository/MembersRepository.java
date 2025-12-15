package com.sungbok.community.repository;

import com.sungbok.community.security.TenantContext;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.generated.tables.daos.MembershipsDao;
import org.jooq.generated.tables.pojos.Memberships;
import org.jooq.generated.tables.records.MembershipsRecord;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.sungbok.community.repository.util.JooqTenantConditionUtils.orgIdCondition;
import static org.jooq.generated.Tables.MEMBERSHIPS;

/**
 * 회원 데이터 접근 Repository
 * 하이브리드 DAO + DSL 패턴 사용
 */
@Repository
public class MembersRepository {

    private final DSLContext dsl;
    private final MembershipsDao dao;

    public MembersRepository(DSLContext dsl, Configuration configuration) {
        this.dsl = dsl;
        this.dao = new MembershipsDao(configuration);
    }

    /**
     * ID로 회원 조회 (app_id 자동 필터링)
     *
     * @param id 회원 ID
     * @return 회원 Optional (없으면 빈 Optional)
     */
    public Optional<Memberships> fetchById(Long id) {
        return dsl.selectFrom(MEMBERSHIPS)
                .where(orgIdCondition(MEMBERSHIPS.ORG_ID))
                .and(MEMBERSHIPS.ID.eq(id))
                .fetchOptionalInto(Memberships.class);
    }

    /**
     * 사용자 ID로 회원 조회 (app_id 자동 필터링)
     *
     * @param userId 사용자 ID
     * @return 회원 Optional (없으면 빈 Optional)
     */
    public Optional<Memberships> fetchByUserId(Long userId) {
         return dsl.selectFrom(MEMBERSHIPS)
                  .where(orgIdCondition(MEMBERSHIPS.ORG_ID))
                  .and(MEMBERSHIPS.USER_ID.eq(userId))
                  .fetchOptionalInto(Memberships.class);
    }

    /**
     * RETURNING 절로 새 회원을 삽입합니다.
     * app_id는 TenantContext에서 자동 설정
     *
     * @param member 삽입할 회원 엔티티
     * @return 모든 필드가 포함된 삽입된 회원
     */
    public Memberships insert(Memberships membership) {
        // TenantContext에서 org_id 가져오기
        Long orgId = TenantContext.getRequiredOrgId();
        membership.setOrgId(orgId);  // 강제로 현재 테넌트 설정

        dao.insert(membership);  // DAO 패턴
        return membership;
    }

    /**
     * Record.store() 패턴으로 회원을 업데이트합니다.
     * org_id로 격리
     *
     * @param membership 업데이트할 회원 엔티티
     */
    public void update(Memberships membership) {
       if (membership == null || membership.getId() == null) {
            throw new IllegalArgumentException("Membership with ID must be provided for update.");
       }

       MembershipsRecord membershipsRecord = dsl.fetchOptional(MEMBERSHIPS,
               orgIdCondition(MEMBERSHIPS.ORG_ID)
                       .and(MEMBERSHIPS.ID.eq(membership.getId())))
           .orElseThrow(() -> new RuntimeException("Membership not found with ID: " + membership.getId()));

       membershipsRecord.setName(membership.getName());
       membershipsRecord.setBirthdate(membership.getBirthdate());
       membershipsRecord.setGender(membership.getGender());
       membershipsRecord.setAddress(membership.getAddress());
       membershipsRecord.setPhoneNumber(membership.getPhoneNumber());
       membershipsRecord.setPicture(membership.getPicture());
       membershipsRecord.setModifiedAt(LocalDateTime.now());
       membershipsRecord.store();
   }

    /**
     * 사용자 ID와 조직 ID로 멤버십 조회 (Guest 사용자 중복 가입 방지)
     * TenantContext 없이 직접 orgId 파라미터로 조회
     *
     * @param userId 사용자 ID
     * @param orgId 조직 ID
     * @return 멤버십 Optional (없으면 빈 Optional)
     */
    public Optional<Memberships> fetchByUserIdAndOrgId(Long userId, Long orgId) {
        return dsl.selectFrom(MEMBERSHIPS)
                .where(MEMBERSHIPS.USER_ID.eq(userId))
                .and(MEMBERSHIPS.ORG_ID.eq(orgId))
                .fetchOptionalInto(Memberships.class);
    }

    /**
     * 멤버십 상태 업데이트 (PENDING → APPROVED/REJECTED)
     *
     * @param membershipId 멤버십 ID
     * @param status 새 상태 (APPROVED/REJECTED)
     * @param approvedBy 승인자 사용자 ID
     * @return 영향받은 행 수
     */
    public int updateStatus(Long membershipId, String status, Long approvedBy) {
        return dsl.update(MEMBERSHIPS)
                .set(MEMBERSHIPS.STATUS, status)
                .set(MEMBERSHIPS.APPROVED_BY, approvedBy)
                .set(MEMBERSHIPS.APPROVED_AT, LocalDateTime.now())
                .set(MEMBERSHIPS.MODIFIED_AT, LocalDateTime.now())
                .where(orgIdCondition(MEMBERSHIPS.ORG_ID))
                .and(MEMBERSHIPS.ID.eq(membershipId))
                .execute();
    }

    /**
     * 조직의 PENDING 멤버십 목록 조회 (관리자용)
     *
     * @return PENDING 멤버십 리스트
     */
    public List<Memberships> fetchPendingMemberships() {
        return dsl.selectFrom(MEMBERSHIPS)
                .where(orgIdCondition(MEMBERSHIPS.ORG_ID))
                .and(MEMBERSHIPS.STATUS.eq("PENDING"))
                .orderBy(MEMBERSHIPS.REQUESTED_AT.desc())
                .fetchInto(Memberships.class);
    }

    /**
     * 현재 사용자의 멤버십 목록 조회 (모든 조직)
     * TenantContext 사용 안 함 (사용자가 아직 조직에 속하지 않을 수 있음)
     *
     * @param userId 사용자 ID
     * @return 멤버십 리스트 (모든 조직)
     */
    public List<Memberships> fetchAllByUserId(Long userId) {
        return dsl.selectFrom(MEMBERSHIPS)
                .where(MEMBERSHIPS.USER_ID.eq(userId))
                .orderBy(MEMBERSHIPS.STATUS.asc(), MEMBERSHIPS.REQUESTED_AT.desc())
                .fetchInto(Memberships.class);
    }

}