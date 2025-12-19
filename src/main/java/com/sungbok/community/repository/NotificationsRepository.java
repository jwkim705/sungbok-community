package com.sungbok.community.repository;

import org.jooq.generated.enums.PushStatus;
import com.sungbok.community.security.TenantContext;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.generated.tables.daos.NotificationsDao;
import org.jooq.generated.tables.pojos.Notifications;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.sungbok.community.repository.util.JooqTenantConditionUtils.orgIdCondition;
import static org.jooq.generated.Tables.NOTIFICATIONS;

/**
 * 알림 데이터 접근 Repository
 * 하이브리드 DAO + DSL 패턴 사용
 */
@Repository
public class NotificationsRepository {

    private final DSLContext dslContext;
    private final NotificationsDao dao;

    public NotificationsRepository(DSLContext dslContext, Configuration configuration) {
        this.dslContext = dslContext;
        this.dao = new NotificationsDao(configuration);
    }

    /**
     * 알림 ID로 조회 (org_id 자동 필터링)
     *
     * @param notificationId 알림 ID
     * @return 알림 Optional (없으면 빈 Optional)
     */
    public Optional<Notifications> fetchById(Long notificationId) {
        return dslContext.selectFrom(NOTIFICATIONS)
                .where(orgIdCondition(NOTIFICATIONS.ORG_ID))
                .and(NOTIFICATIONS.NOTIFICATION_ID.eq(notificationId))
                .and(NOTIFICATIONS.IS_DELETED.eq(false))
                .fetchOptionalInto(Notifications.class);
    }

    /**
     * 알림을 삽입합니다.
     * org_id는 TenantContext에서 자동 설정
     *
     * @param notification 삽입할 알림 엔티티
     * @return 삽입된 알림 (모든 필드 포함)
     */
    public Notifications insert(Notifications notification) {
        // TenantContext에서 org_id 가져오기
        Long orgId = TenantContext.getRequiredOrgId();
        notification.setOrgId(orgId);  // 강제로 현재 테넌트 설정

        dao.insert(notification);  // DAO 패턴
        return notification;
    }

    /**
     * 사용자 ID로 알림 목록 조회 (페이징)
     * org_id 자동 필터링, 생성일시 역순 정렬
     *
     * @param userId 사용자 ID
     * @param limit 조회할 개수
     * @param offset 건너뛸 개수
     * @return 알림 리스트
     */
    public List<Notifications> fetchByUserId(Long userId, int limit, int offset) {
        return dslContext.selectFrom(NOTIFICATIONS)
                .where(orgIdCondition(NOTIFICATIONS.ORG_ID))
                .and(NOTIFICATIONS.USER_ID.eq(userId))
                .and(NOTIFICATIONS.IS_DELETED.eq(false))
                .orderBy(NOTIFICATIONS.CREATED_AT.desc())
                .limit(limit)
                .offset(offset)
                .fetchInto(Notifications.class);
    }

    /**
     * 사용자의 읽지 않은 알림 개수 조회
     * org_id 자동 필터링
     *
     * @param userId 사용자 ID
     * @return 읽지 않은 알림 개수
     */
    public int countUnreadByUserId(Long userId) {
        return dslContext.selectCount()
                .from(NOTIFICATIONS)
                .where(orgIdCondition(NOTIFICATIONS.ORG_ID))
                .and(NOTIFICATIONS.USER_ID.eq(userId))
                .and(NOTIFICATIONS.IS_READ.eq(false))
                .and(NOTIFICATIONS.IS_DELETED.eq(false))
                .fetchOneInto(Integer.class);
    }

    /**
     * 알림을 읽음 처리합니다.
     * org_id 자동 필터링 (사용자 소유 확인)
     *
     * @param notificationId 알림 ID
     * @param userId 사용자 ID (권한 확인용)
     * @return 영향받은 행 수
     */
    public int markAsRead(Long notificationId, Long userId) {
        return dslContext.update(NOTIFICATIONS)
                .set(NOTIFICATIONS.IS_READ, true)
                .set(NOTIFICATIONS.READ_AT, LocalDateTime.now())
                .set(NOTIFICATIONS.MODIFIED_AT, LocalDateTime.now())
                .where(orgIdCondition(NOTIFICATIONS.ORG_ID))
                .and(NOTIFICATIONS.NOTIFICATION_ID.eq(notificationId))
                .and(NOTIFICATIONS.USER_ID.eq(userId))  // 권한 확인
                .and(NOTIFICATIONS.IS_DELETED.eq(false))
                .execute();
    }

    /**
     * 사용자의 모든 알림을 읽음 처리합니다.
     * org_id 자동 필터링
     *
     * @param userId 사용자 ID
     * @return 영향받은 행 수
     */
    public int markAllAsRead(Long userId) {
        return dslContext.update(NOTIFICATIONS)
                .set(NOTIFICATIONS.IS_READ, true)
                .set(NOTIFICATIONS.READ_AT, LocalDateTime.now())
                .set(NOTIFICATIONS.MODIFIED_AT, LocalDateTime.now())
                .where(orgIdCondition(NOTIFICATIONS.ORG_ID))
                .and(NOTIFICATIONS.USER_ID.eq(userId))
                .and(NOTIFICATIONS.IS_READ.eq(false))
                .and(NOTIFICATIONS.IS_DELETED.eq(false))
                .execute();
    }

    /**
     * 푸시 전송 상태를 업데이트합니다.
     * org_id 자동 필터링
     *
     * @param notificationId 알림 ID
     * @param status 푸시 상태 (OK, ERROR, INVALID_TOKEN)
     * @param errorMessage 에러 메시지 (성공 시 null)
     * @return 영향받은 행 수
     */
    public int updatePushStatus(Long notificationId, PushStatus status, String errorMessage) {
        return dslContext.update(NOTIFICATIONS)
                .set(NOTIFICATIONS.PUSH_SENT, true)
                .set(NOTIFICATIONS.PUSH_SENT_AT, LocalDateTime.now())
                .set(NOTIFICATIONS.PUSH_STATUS, status)
                .set(NOTIFICATIONS.PUSH_ERROR_MESSAGE, errorMessage)
                .set(NOTIFICATIONS.MODIFIED_AT, LocalDateTime.now())
                .where(orgIdCondition(NOTIFICATIONS.ORG_ID))
                .and(NOTIFICATIONS.NOTIFICATION_ID.eq(notificationId))
                .execute();
    }

    /**
     * 알림을 소프트 삭제합니다.
     * org_id 자동 필터링 (사용자 소유 확인)
     *
     * @param notificationId 알림 ID
     * @param userId 사용자 ID (권한 확인용)
     * @return 영향받은 행 수
     */
    public int softDelete(Long notificationId, Long userId) {
        return dslContext.update(NOTIFICATIONS)
                .set(NOTIFICATIONS.IS_DELETED, true)
                .set(NOTIFICATIONS.MODIFIED_AT, LocalDateTime.now())
                .where(orgIdCondition(NOTIFICATIONS.ORG_ID))
                .and(NOTIFICATIONS.NOTIFICATION_ID.eq(notificationId))
                .and(NOTIFICATIONS.USER_ID.eq(userId))  // 권한 확인
                .execute();
    }
}
