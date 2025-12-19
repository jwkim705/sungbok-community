package com.sungbok.community.repository;

import tools.jackson.databind.ObjectMapper;
import com.sungbok.community.security.TenantContext;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.generated.tables.daos.NotificationSettingsDao;
import org.jooq.generated.tables.pojos.NotificationSettings;
import org.jooq.generated.tables.records.NotificationSettingsRecord;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.sungbok.community.repository.util.JooqTenantConditionUtils.orgIdCondition;
import static org.jooq.generated.Tables.NOTIFICATION_SETTINGS;

/**
 * 알림 설정 데이터 접근 Repository
 * 하이브리드 DAO + DSL 패턴 사용
 */
@Repository
public class NotificationSettingsRepository {

    private final DSLContext dslContext;
    private final NotificationSettingsDao dao;
    private final ObjectMapper objectMapper;

    public NotificationSettingsRepository(
            DSLContext dslContext,
            Configuration configuration,
            ObjectMapper objectMapper
    ) {
        this.dslContext = dslContext;
        this.dao = new NotificationSettingsDao(configuration);
        this.objectMapper = objectMapper;
    }

    /**
     * 사용자 ID로 알림 설정 조회, 없으면 기본값으로 생성
     * org_id 자동 필터링
     *
     * @param userId 사용자 ID
     * @return 알림 설정 (없으면 기본값 생성 후 반환)
     */
    public NotificationSettings fetchOrCreateByUserId(Long userId) {
        Optional<NotificationSettings> existing = dslContext.selectFrom(NOTIFICATION_SETTINGS)
                .where(orgIdCondition(NOTIFICATION_SETTINGS.ORG_ID))
                .and(NOTIFICATION_SETTINGS.USER_ID.eq(userId))
                .fetchOptionalInto(NotificationSettings.class);

        if (existing.isPresent()) {
            return existing.get();
        }

        // 기본 설정 생성
        return createDefaultSettings(userId);
    }

    /**
     * 기본 알림 설정을 생성합니다.
     * 모든 알림 타입을 활성화한 상태로 초기화
     *
     * @param userId 사용자 ID
     * @return 생성된 알림 설정
     */
    private NotificationSettings createDefaultSettings(Long userId) {
        try {
            // TenantContext에서 org_id 가져오기
            Long orgId = TenantContext.getRequiredOrgId();

            // 기본 알림 설정 (모든 타입 활성화)
            Map<String, Boolean> defaultPreferences = new HashMap<>();
            defaultPreferences.put("post_comment", true);
            defaultPreferences.put("post_like", true);
            defaultPreferences.put("membership_approved", true);
            defaultPreferences.put("membership_rejected", true);
            defaultPreferences.put("admin_announcement", true);

            // Map → JSONB 변환
            String jsonString = objectMapper.writeValueAsString(defaultPreferences);
            JSONB jsonb = JSONB.valueOf(jsonString);

            // 엔티티 생성
            NotificationSettings settings = new NotificationSettings();
            settings.setOrgId(orgId);
            settings.setUserId(userId);
            settings.setNotificationPreferences(jsonb);
            settings.setEnablePushNotifications(true);
            settings.setCreatedAt(LocalDateTime.now());
            settings.setModifiedAt(LocalDateTime.now());

            // DB 삽입
            dao.insert(settings);
            return settings;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create default notification settings", e);
        }
    }

    /**
     * 알림 설정을 업데이트합니다.
     * org_id 자동 필터링 (사용자 소유 확인)
     *
     * @param settings 업데이트할 알림 설정
     * @return 영향받은 행 수
     */
    public int update(NotificationSettings settings) {
        if (settings == null || settings.getUserId() == null) {
            throw new IllegalArgumentException("NotificationSettings with userId must be provided for update.");
        }

        NotificationSettingsRecord record = dslContext.fetchOptional(
                NOTIFICATION_SETTINGS,
                orgIdCondition(NOTIFICATION_SETTINGS.ORG_ID)
                        .and(NOTIFICATION_SETTINGS.USER_ID.eq(settings.getUserId()))
        ).orElseThrow(() -> new RuntimeException(
                "NotificationSettings not found for userId: " + settings.getUserId()
        ));

        // 필드 업데이트
        record.setNotificationPreferences(settings.getNotificationPreferences());
        record.setEnablePushNotifications(settings.getEnablePushNotifications());
        record.setModifiedAt(LocalDateTime.now());
        record.store();  // store() 패턴

        return 1;
    }

    /**
     * 알림 설정을 삭제합니다.
     * org_id 자동 필터링 (사용자 소유 확인)
     *
     * @param userId 사용자 ID
     * @return 영향받은 행 수
     */
    public int delete(Long userId) {
        return dslContext.deleteFrom(NOTIFICATION_SETTINGS)
                .where(orgIdCondition(NOTIFICATION_SETTINGS.ORG_ID))
                .and(NOTIFICATION_SETTINGS.USER_ID.eq(userId))
                .execute();
    }
}
