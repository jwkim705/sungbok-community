package com.sungbok.community.support;

import com.sungbok.community.repository.AppTypesRepository;
import com.sungbok.community.repository.OrganizationsRepository;
import com.sungbok.community.repository.RolesRepository;
import com.sungbok.community.security.TenantContext;
import org.jooq.DSLContext;
import org.jooq.generated.enums.OrganizationStatus;
import org.jooq.generated.tables.pojos.AppTypes;
import org.jooq.generated.tables.pojos.Organizations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static org.jooq.generated.Tables.ROLES;

/**
 * 테스트 데이터 생성 및 ThreadLocal 관리
 * TestDataHelper를 대체하며 Fixture 패턴과 통합
 *
 * 개선사항:
 * 1. jOOQ 안티패턴 제거 (Repository 패턴 사용)
 * 2. 로그 한국어 변환
 * 3. Fixture 패턴과 통합
 */
@Component
public class TestDataManager {

    private static final Logger log = LoggerFactory.getLogger(TestDataManager.class);

    private final DSLContext dsl;
    private final AppTypesRepository appTypesRepository;
    private final OrganizationsRepository organizationsRepository;
    private final RolesRepository rolesRepository;

    // ThreadLocal로 테스트 간 데이터 격리
    private static final ThreadLocal<Long> testAppTypeId = new ThreadLocal<>();
    private static final ThreadLocal<Long> testOrgId = new ThreadLocal<>();
    private static final ThreadLocal<Long> testRoleId = new ThreadLocal<>();

    public TestDataManager(
            DSLContext dsl,
            AppTypesRepository appTypesRepository,
            OrganizationsRepository organizationsRepository,
            RolesRepository rolesRepository
    ) {
        this.dsl = dsl;
        this.appTypesRepository = appTypesRepository;
        this.organizationsRepository = organizationsRepository;
        this.rolesRepository = rolesRepository;
    }

    /**
     * 모든 테스트 데이터 ID 초기화
     */
    public void clearTestData() {
        testAppTypeId.remove();
        testOrgId.remove();
        testRoleId.remove();
    }

    /**
     * 테스트에 필요한 기본 데이터 생성
     *
     * @return 생성된 organization ID
     */
    public Long ensureTestDataExists() {
        log.info("[TestDataManager] 테스트 데이터 생성 시작...");
        ensureAppType();
        log.info("[TestDataManager] AppType ID: {}", testAppTypeId.get());
        ensureOrganization();
        log.info("[TestDataManager] Organization ID: {}", testOrgId.get());
        ensureRole();
        log.info("[TestDataManager] Role ID: {}", testRoleId.get());
        return getTestOrgId();
    }

    /**
     * 테스트 AppType ID 조회
     *
     * @return AppType ID (ThreadLocal)
     */
    public static Long getTestAppTypeId() {
        return testAppTypeId.get();
    }

    /**
     * 테스트 Organization ID 조회
     *
     * @return Organization ID (ThreadLocal)
     */
    public static Long getTestOrgId() {
        return testOrgId.get();
    }

    /**
     * 테스트 Role ID 조회
     *
     * @return Role ID (ThreadLocal)
     */
    public static Long getTestRoleId() {
        return testRoleId.get();
    }

    /**
     * AppType 생성 또는 조회
     */
    private void ensureAppType() {
        if (testAppTypeId.get() == null) {
            AppTypes appType = appTypesRepository.fetchByName("Test App Type")
                    .orElse(null);

            if (appType == null) {
                AppTypes newAppType = new AppTypes();
                newAppType.setName("Test App Type");
                newAppType.setDescription("단위 테스트용 앱 타입");
                newAppType.setIsActive(true);
                newAppType.setCreatedAt(LocalDateTime.now());
                newAppType.setModifiedAt(LocalDateTime.now());

                appType = appTypesRepository.insert(newAppType);  // Repository 패턴
            }
            testAppTypeId.set(appType.getAppTypeId());
        }
    }

    /**
     * Organization 생성 또는 조회
     */
    private void ensureOrganization() {
        if (testOrgId.get() == null) {
            log.info("[TestDataManager] 기존 조직 조회 중...");
            Organizations org = organizationsRepository.fetchByOrgKey("test-org")
                    .orElse(null);

            if (org == null) {
                log.info("[TestDataManager] 새 조직 생성 중, app_type_id={}", testAppTypeId.get());
                Organizations newOrg = new Organizations();
                newOrg.setAppTypeId(testAppTypeId.get());
                newOrg.setOrgName("테스트 조직");
                newOrg.setOrgKey("test-org");
                newOrg.setIsPublic(true);
                newOrg.setStatus(OrganizationStatus.ACTIVE);
                newOrg.setCreatedAt(LocalDateTime.now());
                newOrg.setCreatedBy(1L);
                newOrg.setModifiedAt(LocalDateTime.now());
                newOrg.setModifiedBy(1L);

                org = organizationsRepository.insert(newOrg);  // Repository 패턴
                log.info("[TestDataManager] 조직 생성 완료, org_id={}", org.getOrgId());
            } else {
                log.info("[TestDataManager] 기존 조직 발견, org_id={}", org.getOrgId());
            }
            testOrgId.set(org.getOrgId());
        } else {
            log.info("[TestDataManager] ThreadLocal에 조직 이미 존재, org_id={}", testOrgId.get());
        }
    }

    /**
     * Role 생성 또는 조회
     * DSL로 직접 처리 (간단한 INSERT)
     */
    private void ensureRole() {
        if (testRoleId.get() == null) {
            Long orgId = testOrgId.get();
            var role = dsl.selectFrom(ROLES)
                    .where(ROLES.NAME.eq("member"))
                    .and(ROLES.ORG_ID.eq(orgId))
                    .fetchOne();

            if (role == null) {
                Long roleId = dsl.insertInto(ROLES)
                        .set(ROLES.NAME, "member")
                        .set(ROLES.ORG_ID, orgId)
                        .set(ROLES.DESCRIPTION, "테스트용 기본 회원 역할")
                        .set(ROLES.LEVEL, 1)
                        .returning(ROLES.ID)
                        .fetchOne()
                        .getId();
                testRoleId.set(roleId);
            } else {
                testRoleId.set(role.getId());
            }
        }
    }
}
