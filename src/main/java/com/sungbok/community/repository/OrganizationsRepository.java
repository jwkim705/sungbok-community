package com.sungbok.community.repository;

import org.jooq.generated.enums.OrganizationStatus;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.generated.tables.daos.OrganizationsDao;
import org.jooq.generated.tables.pojos.Organizations;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static org.jooq.generated.Tables.ORGANIZATIONS;

/**
 * Organizations (조직) 데이터 접근 Repository
 * 하이브리드 DAO + DSL 패턴 사용
 *
 * @since 0.0.1
 */
@Repository
public class OrganizationsRepository {

    private final DSLContext dsl;
    private final OrganizationsDao dao;

    public OrganizationsRepository(DSLContext dsl, Configuration configuration) {
        this.dsl = dsl;
        this.dao = new OrganizationsDao(configuration);
    }

    /**
     * org_id로 조직 조회
     *
     * @param orgId 조직 ID
     * @return 조직 Optional (없으면 빈 Optional)
     */
    public Optional<Organizations> fetchById(Long orgId) {
        return Optional.ofNullable(dao.findById(orgId));
    }

    /**
     * org_key로 조직 조회
     *
     * @param orgKey 조직 키 (고유 식별자)
     * @return 조직 Optional (없으면 빈 Optional)
     */
    public Optional<Organizations> fetchByOrgKey(String orgKey) {
        return dsl.selectFrom(ORGANIZATIONS)
                .where(ORGANIZATIONS.ORG_KEY.eq(orgKey))
                .and(ORGANIZATIONS.STATUS.eq(OrganizationStatus.ACTIVE))
                .fetchOptionalInto(Organizations.class);
    }

    /**
     * 모든 공개 조직 조회 (Guest mode용)
     *
     * @return 공개 조직 리스트
     */
    public List<Organizations> fetchAllPublic() {
        return dsl.selectFrom(ORGANIZATIONS)
                .where(ORGANIZATIONS.IS_PUBLIC.eq(true))
                .and(ORGANIZATIONS.STATUS.eq(OrganizationStatus.ACTIVE))
                .fetchInto(Organizations.class);
    }

    /**
     * 앱 타입별 공개 조직 조회
     *
     * @param appTypeId 앱 타입 ID
     * @return 공개 조직 리스트
     */
    public List<Organizations> fetchByAppType(Long appTypeId) {
        return dsl.selectFrom(ORGANIZATIONS)
                .where(ORGANIZATIONS.APP_TYPE_ID.eq(appTypeId))
                .and(ORGANIZATIONS.IS_PUBLIC.eq(true))
                .and(ORGANIZATIONS.STATUS.eq(OrganizationStatus.ACTIVE))
                .fetchInto(Organizations.class);
    }

    /**
     * 새 조직 삽입
     * RETURNING 절로 생성된 ID 반환
     *
     * @param org 삽입할 조직 엔티티
     * @return 생성된 org_id가 포함된 조직
     */
    public Organizations insert(Organizations org) {
        dao.insert(org);  // DAO 패턴
        return org;
    }
}
