package com.sungbok.community.service.change;

import com.sungbok.community.dto.NotificationSettingsDTO;
import com.sungbok.community.dto.PushTokenRequest;

/**
 * 알림 변경 서비스 인터페이스 (CQRS - Command)
 * 알림 읽음 처리, Push Token 등록/삭제, 알림 설정 변경
 */
public interface ChangeNotificationService {

    /**
     * 알림을 읽음 처리합니다
     * org_id 자동 필터링 (사용자 소유 확인)
     *
     * @param notificationId 알림 ID
     * @param userId 사용자 ID (권한 확인용)
     */
    void markAsRead(Long notificationId, Long userId);

    /**
     * 사용자의 모든 알림을 읽음 처리합니다
     * org_id 자동 필터링
     *
     * @param userId 사용자 ID
     */
    void markAllAsRead(Long userId);

    /**
     * Push Token을 등록합니다 (Upsert)
     * org_id 자동 설정, Valkey 캐시 갱신
     *
     * @param userId 사용자 ID
     * @param request Push Token 등록 요청
     */
    void registerPushToken(Long userId, PushTokenRequest request);

    /**
     * Push Token을 삭제합니다
     * Valkey 9 Hash Field Expiration 사용 (특정 기기만 삭제)
     * org_id 자동 필터링, Valkey 캐시 무효화
     *
     * @param userId   사용자 ID
     * @param deviceId 삭제할 기기 ID
     */
    void unregisterPushToken(Long userId, String deviceId);

    /**
     * 알림 설정을 변경합니다
     * org_id 자동 필터링, Valkey 캐시 무효화
     *
     * @param userId 사용자 ID
     * @param settingsDTO 알림 설정 DTO
     */
    void updateNotificationSettings(Long userId, NotificationSettingsDTO settingsDTO);
}
