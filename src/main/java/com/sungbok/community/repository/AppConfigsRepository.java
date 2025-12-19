package com.sungbok.community.repository;

import com.sungbok.community.security.TenantContext;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.generated.enums.ConfigType;
import org.jooq.generated.tables.daos.AppConfigsDao;
import org.jooq.generated.tables.pojos.AppConfigs;
import org.jooq.generated.tables.records.AppConfigsRecord;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.sungbok.community.repository.util.JooqTenantConditionUtils.orgIdCondition;
import static org.jooq.generated.Tables.APP_CONFIGS;

/**
 * 앱 설정 데이터 접근 Repository
 * 조직별 동적 설정 관리 (UI 테마, 환영 메시지 등)
 * 하이브리드 DAO + DSL 패턴 사용
 */
@Repository
public class AppConfigsRepository {

    private final DSLContext dslContext;
    private final AppConfigsDao dao;

    public AppConfigsRepository(DSLContext dslContext, Configuration configuration) {
        this.dslContext = dslContext;
        this.dao = new AppConfigsDao(configuration);
    }

    /**
     * config_key로 앱 설정 조회
     * org_id 자동 필터링
     *
     * @param configKey 설정 키 (예: theme_primary_color, logo_url)
     * @return 앱 설정 Optional (없으면 빈 Optional)
     */
    public Optional<AppConfigs> fetchByOrgIdAndKey(String configKey) {
        return dslContext.selectFrom(APP_CONFIGS)
                .where(orgIdCondition(APP_CONFIGS.ORG_ID))
                .and(APP_CONFIGS.CONFIG_KEY.eq(configKey))
                .fetchOptionalInto(AppConfigs.class);
    }

    /**
     * 여러 config_key로 앱 설정 리스트 조회
     * org_id 자동 필터링
     *
     * @param configKeys 설정 키 리스트
     * @return 앱 설정 리스트
     */
    public List<AppConfigs> fetchByOrgIdAndKeys(List<String> configKeys) {
        return dslContext.selectFrom(APP_CONFIGS)
                .where(orgIdCondition(APP_CONFIGS.ORG_ID))
                .and(APP_CONFIGS.CONFIG_KEY.in(configKeys))
                .fetchInto(AppConfigs.class);
    }

    /**
     * 조직의 모든 앱 설정 조회
     * org_id 자동 필터링
     *
     * @return 앱 설정 리스트
     */
    public List<AppConfigs> fetchAllByOrgId() {
        return dslContext.selectFrom(APP_CONFIGS)
                .where(orgIdCondition(APP_CONFIGS.ORG_ID))
                .orderBy(APP_CONFIGS.CONFIG_KEY.asc())
                .fetchInto(AppConfigs.class);
    }

    /**
     * 앱 설정을 Upsert합니다. (INSERT or UPDATE)
     * Record.store() 패턴 사용
     * org_id는 TenantContext에서 자동 설정
     *
     * UNIQUE 제약: (org_id, config_key)가 중복되면 UPDATE, 없으면 INSERT
     *
     * @param configKey 설정 키
     * @param configValue 설정 값
     * @param configType 설정 값 타입 (string, integer, boolean, json, array, date)
     * @param description 설명
     */
    public void upsert(String configKey, String configValue, ConfigType configType, String description) {
        // TenantContext에서 org_id 가져오기
        Long orgId = TenantContext.getRequiredOrgId();

        // 기존 레코드 조회
        Optional<AppConfigsRecord> existingRecord = dslContext.fetchOptional(
                APP_CONFIGS,
                orgIdCondition(APP_CONFIGS.ORG_ID)
                        .and(APP_CONFIGS.CONFIG_KEY.eq(configKey))
        );

        LocalDateTime now = LocalDateTime.now();

        if (existingRecord.isPresent()) {
            // UPDATE
            AppConfigsRecord record = existingRecord.get();
            record.setConfigValue(configValue);
            record.setConfigType(configType);
            record.setDescription(description);
            record.setModifiedAt(now);
            record.store();  // store() 패턴
        } else {
            // INSERT
            AppConfigsRecord record = dslContext.newRecord(APP_CONFIGS);
            record.setOrgId(orgId);
            record.setConfigKey(configKey);
            record.setConfigValue(configValue);
            record.setConfigType(configType);
            record.setDescription(description);
            record.setCreatedAt(now);
            record.setModifiedAt(now);
            record.store();  // store() 패턴
        }
    }

    /**
     * 앱 설정을 삽입합니다.
     * org_id는 TenantContext에서 자동 설정
     *
     * @param appConfig 삽입할 앱 설정 엔티티
     * @return 삽입된 앱 설정 (모든 필드 포함)
     */
    public AppConfigs insert(AppConfigs appConfig) {
        // TenantContext에서 org_id 가져오기
        Long orgId = TenantContext.getRequiredOrgId();
        appConfig.setOrgId(orgId);  // 강제로 현재 테넌트 설정

        // 현재 시간 설정
        LocalDateTime now = LocalDateTime.now();
        appConfig.setCreatedAt(now);
        appConfig.setModifiedAt(now);

        dao.insert(appConfig);  // DAO 패턴
        return appConfig;
    }

    /**
     * 앱 설정을 삭제합니다.
     * org_id 자동 필터링
     *
     * @param configKey 설정 키
     * @return 영향받은 행 수
     */
    public int delete(String configKey) {
        return dslContext.deleteFrom(APP_CONFIGS)
                .where(orgIdCondition(APP_CONFIGS.ORG_ID))
                .and(APP_CONFIGS.CONFIG_KEY.eq(configKey))
                .execute();
    }

    /**
     * 조직의 모든 앱 설정을 삭제합니다.
     * org_id 자동 필터링
     *
     * @return 영향받은 행 수
     */
    public int deleteAllByOrgId() {
        return dslContext.deleteFrom(APP_CONFIGS)
                .where(orgIdCondition(APP_CONFIGS.ORG_ID))
                .execute();
    }
}
