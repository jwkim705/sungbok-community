package com.sungbok.community.repository;

import com.sungbok.community.security.TenantContext;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.generated.tables.daos.PushTokensDao;
import org.jooq.generated.tables.pojos.PushTokens;
import org.jooq.generated.tables.records.PushTokensRecord;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static com.sungbok.community.repository.util.JooqTenantConditionUtils.orgIdCondition;
import static org.jooq.generated.Tables.PUSH_TOKENS;

/**
 * Push Token 데이터 접근 Repository
 * 하이브리드 DAO + DSL 패턴 사용
 */
@Repository
public class PushTokensRepository {

    private final DSLContext dslContext;
    private final PushTokensDao dao;

    public PushTokensRepository(DSLContext dslContext, Configuration configuration) {
        this.dslContext = dslContext;
        this.dao = new PushTokensDao(configuration);
    }

    /**
     * Push Token을 등록하거나 업데이트합니다 (Upsert)
     * org_id는 TenantContext에서 자동 설정
     * 기존 토큰이 있으면 last_used_at 갱신, 없으면 신규 삽입
     *
     * @param pushToken 등록할 Push Token 엔티티
     * @return 저장된 Push Token
     */
    public PushTokens upsert(PushTokens pushToken) {
        // TenantContext에서 org_id 가져오기
        Long orgId = TenantContext.getRequiredOrgId();
        pushToken.setOrgId(orgId);  // 강제로 현재 테넌트 설정

        // 기존 토큰 조회
        PushTokensRecord existingRecord = dslContext
                .selectFrom(PUSH_TOKENS)
                .where(orgIdCondition(PUSH_TOKENS.ORG_ID))
                .and(PUSH_TOKENS.USER_ID.eq(pushToken.getUserId()))
                .and(PUSH_TOKENS.EXPO_PUSH_TOKEN.eq(pushToken.getExpoPushToken()))
                .fetchOne();

        if (existingRecord != null) {
            // 업데이트: last_used_at, device 정보 갱신
            existingRecord.setDeviceType(pushToken.getDeviceType());
            existingRecord.setDeviceName(pushToken.getDeviceName());
            existingRecord.setAppVersion(pushToken.getAppVersion());
            existingRecord.setIsActive(true);
            existingRecord.setLastUsedAt(LocalDateTime.now());
            existingRecord.setModifiedAt(LocalDateTime.now());
            existingRecord.store();  // store() 패턴
            return existingRecord.into(PushTokens.class);
        } else {
            // 신규 삽입
            pushToken.setIsActive(true);
            pushToken.setLastUsedAt(LocalDateTime.now());
            dao.insert(pushToken);  // DAO 패턴
            return pushToken;
        }
    }

    /**
     * 사용자의 활성화된 Push Token 목록 조회
     * org_id 자동 필터링
     *
     * @param userId 사용자 ID
     * @return 활성화된 Push Token 리스트
     */
    public List<PushTokens> fetchActiveByUserId(Long userId) {
        return dslContext.selectFrom(PUSH_TOKENS)
                .where(orgIdCondition(PUSH_TOKENS.ORG_ID))
                .and(PUSH_TOKENS.USER_ID.eq(userId))
                .and(PUSH_TOKENS.IS_ACTIVE.eq(true))
                .and(PUSH_TOKENS.IS_DELETED.eq(false))
                .orderBy(PUSH_TOKENS.LAST_USED_AT.desc())
                .fetchInto(PushTokens.class);
    }

    /**
     * Expo Push Token을 비활성화합니다.
     * Expo API에서 DeviceNotRegistered 에러 발생 시 호출
     * org_id 자동 필터링
     *
     * @param expoPushToken 비활성화할 Expo Push Token
     * @return 영향받은 행 수
     */
    public int deactivateToken(String expoPushToken) {
        return dslContext.update(PUSH_TOKENS)
                .set(PUSH_TOKENS.IS_ACTIVE, false)
                .set(PUSH_TOKENS.MODIFIED_AT, LocalDateTime.now())
                .where(orgIdCondition(PUSH_TOKENS.ORG_ID))
                .and(PUSH_TOKENS.EXPO_PUSH_TOKEN.eq(expoPushToken))
                .execute();
    }

    /**
     * 사용자의 특정 Push Token을 소프트 삭제합니다.
     * 로그아웃 또는 토큰 삭제 요청 시 사용
     * org_id 자동 필터링 (사용자 소유 확인)
     *
     * @param userId 사용자 ID (권한 확인용)
     * @param expoPushToken 삭제할 Expo Push Token
     * @return 영향받은 행 수
     */
    public int softDelete(Long userId, String expoPushToken) {
        return dslContext.update(PUSH_TOKENS)
                .set(PUSH_TOKENS.IS_DELETED, true)
                .set(PUSH_TOKENS.MODIFIED_AT, LocalDateTime.now())
                .where(orgIdCondition(PUSH_TOKENS.ORG_ID))
                .and(PUSH_TOKENS.USER_ID.eq(userId))  // 권한 확인
                .and(PUSH_TOKENS.EXPO_PUSH_TOKEN.eq(expoPushToken))
                .execute();
    }

    /**
     * 사용자의 모든 Push Token을 소프트 삭제합니다.
     * 회원 탈퇴 또는 전체 디바이스 로그아웃 시 사용
     * org_id 자동 필터링
     *
     * @param userId 사용자 ID
     * @return 영향받은 행 수
     */
    public int softDeleteAllByUserId(Long userId) {
        return dslContext.update(PUSH_TOKENS)
                .set(PUSH_TOKENS.IS_DELETED, true)
                .set(PUSH_TOKENS.MODIFIED_AT, LocalDateTime.now())
                .where(orgIdCondition(PUSH_TOKENS.ORG_ID))
                .and(PUSH_TOKENS.USER_ID.eq(userId))
                .execute();
    }
}
