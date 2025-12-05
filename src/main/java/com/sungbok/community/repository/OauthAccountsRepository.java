package com.sungbok.community.repository;

import com.sungbok.community.enums.SocialType;
import com.sungbok.community.security.TenantContext;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.generated.tables.daos.OauthAccountsDao;
import org.jooq.generated.tables.pojos.OauthAccounts;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.sungbok.community.repository.util.JooqTenantConditionUtils.appIdCondition;
import static org.jooq.generated.Tables.OAUTH_ACCOUNTS;

/**
 * OAuth 계정 데이터 접근 Repository
 * 하이브리드 DAO + DSL 패턴 사용
 */
@Repository
public class OauthAccountsRepository {

    private final DSLContext dsl;
    private final OauthAccountsDao dao;

    public OauthAccountsRepository(DSLContext dsl, Configuration configuration) {
        this.dsl = dsl;
        this.dao = new OauthAccountsDao(configuration);
    }

    /**
     * RETURNING 절로 OAuth 계정을 삽입합니다.
     * app_id는 TenantContext에서 자동 설정
     *
     * @param userId 사용자 ID
     * @param socialType 소셜 타입 (GOOGLE, KAKAO, NAVER)
     * @return 삽입된 OAuth 계정
     */
    public OauthAccounts insert(Long userId, SocialType socialType) {
        // TenantContext에서 app_id 가져오기
        Long appId = TenantContext.getRequiredAppId();

        return dsl.insertInto(OAUTH_ACCOUNTS)
                .set(OAUTH_ACCOUNTS.APP_ID, appId)  // 강제로 현재 테넌트 설정
                .set(OAUTH_ACCOUNTS.USER_ID, userId)
                .set(OAUTH_ACCOUNTS.SOCIAL_TYPE, socialType.getCode())
                .set(OAUTH_ACCOUNTS.IS_DELETED, false)
                .set(OAUTH_ACCOUNTS.CREATED_AT, LocalDateTime.now())
                .set(OAUTH_ACCOUNTS.MODIFIED_AT, LocalDateTime.now())
                .returning()
                .fetchOneInto(OauthAccounts.class);
    }

    /**
     * 사용자의 모든 OAuth 계정을 조회합니다.
     * app_id로 격리
     *
     * @param userId 사용자 ID
     * @return OAuth 계정 리스트
     */
    public List<OauthAccounts> fetchByUserId(Long userId) {
        return dsl.selectFrom(OAUTH_ACCOUNTS)
                .where(appIdCondition(OAUTH_ACCOUNTS.APP_ID))
                .and(OAUTH_ACCOUNTS.USER_ID.eq(userId))
                .and(OAUTH_ACCOUNTS.IS_DELETED.eq(false))
                .fetchInto(OauthAccounts.class);
    }

    /**
     * 사용자의 특정 소셜 타입 계정을 조회합니다.
     * app_id로 격리
     *
     * @param userId 사용자 ID
     * @param socialType 소셜 타입
     * @return Optional로 감싼 OauthAccounts
     */
    public Optional<OauthAccounts> fetchByUserIdAndSocialType(Long userId, SocialType socialType) {
        return dsl.selectFrom(OAUTH_ACCOUNTS)
                .where(appIdCondition(OAUTH_ACCOUNTS.APP_ID))
                .and(OAUTH_ACCOUNTS.USER_ID.eq(userId))
                .and(OAUTH_ACCOUNTS.SOCIAL_TYPE.eq(socialType.getCode()))
                .and(OAUTH_ACCOUNTS.IS_DELETED.eq(false))
                .fetchOptionalInto(OauthAccounts.class);
    }

    /**
     * DSLContext.update()로 OAuth 계정을 소프트 삭제합니다.
     * app_id로 격리
     *
     * @param id OAuth 계정 ID
     * @return 영향받은 행 수
     */
    public int softDelete(Long id) {
        return dsl.update(OAUTH_ACCOUNTS)
                .set(OAUTH_ACCOUNTS.IS_DELETED, true)
                .set(OAUTH_ACCOUNTS.MODIFIED_AT, LocalDateTime.now())
                .where(appIdCondition(OAUTH_ACCOUNTS.APP_ID))
                .and(OAUTH_ACCOUNTS.ID.eq(id))
                .execute();
    }
}
