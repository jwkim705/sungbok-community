package com.sungbok.community.repository;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.generated.enums.OrganizationStatus;
import org.jooq.generated.tables.daos.OrganizationsDao;
import org.jooq.generated.tables.pojos.Organizations;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static org.jooq.generated.Tables.ORGANIZATIONS;

/**
 * Apps (테넌트) 데이터 접근 Repository
 * 하이브리드 DAO + DSL 패턴 사용
 *
 * @since 0.0.1
 */
@Repository
public class AppsRepository {

    private final DSLContext dsl;
    private final OrganizationsDao dao;

    public AppsRepository(DSLContext dsl, Configuration configuration) {
        this.dsl = dsl;
        this.dao = new OrganizationsDao(configuration);
    }

    /**
     * app_id로 앱 조회
     *
     * @param orgId 앱 ID
     * @return 앱 Optional (없으면 빈 Optional)
     */
    public Optional<Organizations> fetchById(Long orgId) {
        return Optional.ofNullable(dao.findById(orgId));
    }

    /**
     * app_key로 앱 조회
     *
     * @param appKey 앱 키 (고유 식별자)
     * @return 앱 Optional (없으면 빈 Optional)
     */
    public Optional<Organizations> fetchByAppKey(String appKey) {
        return dsl.selectFrom(ORGANIZATIONS)
                .where(ORGANIZATIONS.ORG_KEY.eq(appKey))
                .and(ORGANIZATIONS.STATUS.eq(OrganizationStatus.ACTIVE))
                .fetchOptionalInto(Organizations.class);
    }

    /**
     * 새 앱 삽입
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
