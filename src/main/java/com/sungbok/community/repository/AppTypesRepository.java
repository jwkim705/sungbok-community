package com.sungbok.community.repository;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.generated.tables.daos.AppTypesDao;
import org.jooq.generated.tables.pojos.AppTypes;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static org.jooq.generated.Tables.APP_TYPES;

/**
 * AppTypes (앱 타입) 데이터 접근 Repository
 * 하이브리드 DAO + DSL 패턴 사용
 *
 * @since 0.0.1
 */
@Repository
public class AppTypesRepository {

    private final DSLContext dsl;
    private final AppTypesDao dao;

    public AppTypesRepository(DSLContext dsl, Configuration configuration) {
        this.dsl = dsl;
        this.dao = new AppTypesDao(configuration);
    }

    /**
     * app_type_id로 앱 타입 조회
     *
     * @param appTypeId 앱 타입 ID
     * @return 앱 타입 Optional (없으면 빈 Optional)
     */
    public Optional<AppTypes> fetchById(Long appTypeId) {
        return Optional.ofNullable(dao.findById(appTypeId));
    }

    /**
     * name으로 앱 타입 조회
     *
     * @param name 앱 타입 이름
     * @return 앱 타입 Optional (없으면 빈 Optional)
     */
    public Optional<AppTypes> fetchByName(String name) {
        return dsl.selectFrom(APP_TYPES)
                .where(APP_TYPES.NAME.eq(name))
                .and(APP_TYPES.IS_ACTIVE.eq(true))
                .fetchOptionalInto(AppTypes.class);
    }

    /**
     * 모든 활성 앱 타입 조회
     *
     * @return 활성 앱 타입 리스트
     */
    public List<AppTypes> fetchAllActive() {
        return dsl.selectFrom(APP_TYPES)
                .where(APP_TYPES.IS_ACTIVE.eq(true))
                .orderBy(APP_TYPES.APP_TYPE_ID.asc())
                .fetchInto(AppTypes.class);
    }

    /**
     * 새 앱 타입 삽입
     * RETURNING 절로 생성된 ID 반환
     *
     * @param appType 삽입할 앱 타입 엔티티
     * @return 생성된 app_type_id가 포함된 앱 타입
     */
    public AppTypes insert(AppTypes appType) {
        dao.insert(appType);  // DAO 패턴
        return appType;
    }
}
