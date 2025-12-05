package com.sungbok.community.repository;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.generated.tables.daos.AppsDao;
import org.jooq.generated.tables.pojos.Apps;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static org.jooq.generated.Tables.APPS;

/**
 * Apps (테넌트) 데이터 접근 Repository
 * 하이브리드 DAO + DSL 패턴 사용
 *
 * @since 0.0.1
 */
@Repository
public class AppsRepository {

    private final DSLContext dsl;
    private final AppsDao dao;

    public AppsRepository(DSLContext dsl, Configuration configuration) {
        this.dsl = dsl;
        this.dao = new AppsDao(configuration);
    }

    /**
     * app_id로 앱 조회
     *
     * @param appId 앱 ID
     * @return 앱 Optional (없으면 빈 Optional)
     */
    public Optional<Apps> fetchById(Long appId) {
        return Optional.ofNullable(dao.findById(appId));
    }

    /**
     * app_key로 앱 조회
     *
     * @param appKey 앱 키 (고유 식별자)
     * @return 앱 Optional (없으면 빈 Optional)
     */
    public Optional<Apps> fetchByAppKey(String appKey) {
        return dsl.selectFrom(APPS)
                .where(APPS.APP_KEY.eq(appKey))
                .and(APPS.STATUS.eq("ACTIVE"))
                .fetchOptionalInto(Apps.class);
    }

    /**
     * 새 앱 삽입
     * RETURNING 절로 생성된 ID 반환
     *
     * @param app 삽입할 앱 엔티티
     * @return 생성된 app_id가 포함된 앱
     */
    public Apps insert(Apps app) {
        return dsl.insertInto(APPS)
                .set(dsl.newRecord(APPS, app))
                .returning()
                .fetchOneInto(Apps.class);
    }
}
