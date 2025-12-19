package com.sungbok.community.repository;

import com.sungbok.community.security.TenantContext;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.generated.enums.PlatformType;
import org.jooq.generated.tables.daos.AppVersionsDao;
import org.jooq.generated.tables.pojos.AppVersions;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.sungbok.community.repository.util.JooqTenantConditionUtils.orgIdCondition;
import static org.jooq.generated.Tables.APP_VERSIONS;

/**
 * 앱 버전 관리 데이터 접근 Repository
 * 조직별 앱 버전 관리 (강제 업데이트, 점검 모드)
 * 하이브리드 DAO + DSL 패턴 사용
 */
@Repository
public class AppVersionsRepository {

    private final DSLContext dslContext;
    private final AppVersionsDao dao;

    public AppVersionsRepository(DSLContext dslContext, Configuration configuration) {
        this.dslContext = dslContext;
        this.dao = new AppVersionsDao(configuration);
    }

    /**
     * 조직과 플랫폼으로 앱 버전 조회
     * org_id 자동 필터링
     *
     * @param platform 플랫폼 타입 (ios, android)
     * @return 앱 버전 Optional (없으면 빈 Optional)
     */
    public Optional<AppVersions> fetchByOrgAndPlatform(PlatformType platform) {
        return dslContext.selectFrom(APP_VERSIONS)
                .where(orgIdCondition(APP_VERSIONS.ORG_ID))
                .and(APP_VERSIONS.PLATFORM.eq(platform))
                .fetchOptionalInto(AppVersions.class);
    }

    /**
     * 앱 버전 정보를 삽입합니다.
     * org_id는 TenantContext에서 자동 설정
     *
     * @param appVersion 삽입할 앱 버전 엔티티
     * @return 삽입된 앱 버전 (모든 필드 포함)
     */
    public AppVersions insert(AppVersions appVersion) {
        // TenantContext에서 org_id 가져오기
        Long orgId = TenantContext.getRequiredOrgId();
        appVersion.setOrgId(orgId);  // 강제로 현재 테넌트 설정

        // 현재 시간 설정
        LocalDateTime now = LocalDateTime.now();
        appVersion.setCreatedAt(now);
        appVersion.setModifiedAt(now);

        dao.insert(appVersion);  // DAO 패턴
        return appVersion;
    }

    /**
     * 앱 버전 정보를 업데이트합니다.
     * org_id 자동 필터링
     *
     * @param platform 플랫폼 타입
     * @param minVersion 최소 지원 버전
     * @param latestVersion 최신 버전
     * @param forceUpdateMessage 강제 업데이트 메시지
     * @param updateUrl 업데이트 URL (앱스토어/플레이스토어)
     * @param modifiedBy 수정자 ID
     * @return 영향받은 행 수
     */
    public int updateVersion(
            PlatformType platform,
            String minVersion,
            String latestVersion,
            String forceUpdateMessage,
            String updateUrl,
            Long modifiedBy
    ) {
        return dslContext.update(APP_VERSIONS)
                .set(APP_VERSIONS.MIN_VERSION, minVersion)
                .set(APP_VERSIONS.LATEST_VERSION, latestVersion)
                .set(APP_VERSIONS.FORCE_UPDATE_MESSAGE, forceUpdateMessage)
                .set(APP_VERSIONS.UPDATE_URL, updateUrl)
                .set(APP_VERSIONS.MODIFIED_AT, LocalDateTime.now())
                .set(APP_VERSIONS.MODIFIED_BY, modifiedBy)
                .where(orgIdCondition(APP_VERSIONS.ORG_ID))
                .and(APP_VERSIONS.PLATFORM.eq(platform))
                .execute();
    }

    /**
     * 점검 모드를 업데이트합니다.
     * org_id 자동 필터링
     *
     * @param platform 플랫폼 타입
     * @param isMaintenance 점검 모드 활성화 여부
     * @param message 점검 메시지
     * @param modifiedBy 수정자 ID
     * @return 영향받은 행 수
     */
    public int updateMaintenanceMode(
            PlatformType platform,
            boolean isMaintenance,
            String message,
            Long modifiedBy
    ) {
        return dslContext.update(APP_VERSIONS)
                .set(APP_VERSIONS.IS_MAINTENANCE, isMaintenance)
                .set(APP_VERSIONS.MAINTENANCE_MESSAGE, message)
                .set(APP_VERSIONS.MODIFIED_AT, LocalDateTime.now())
                .set(APP_VERSIONS.MODIFIED_BY, modifiedBy)
                .where(orgIdCondition(APP_VERSIONS.ORG_ID))
                .and(APP_VERSIONS.PLATFORM.eq(platform))
                .execute();
    }

    /**
     * 조직의 모든 앱 버전 정보를 삭제합니다.
     * org_id 자동 필터링
     *
     * @return 영향받은 행 수
     */
    public int deleteByOrg() {
        return dslContext.deleteFrom(APP_VERSIONS)
                .where(orgIdCondition(APP_VERSIONS.ORG_ID))
                .execute();
    }
}
