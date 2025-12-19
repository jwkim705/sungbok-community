package com.sungbok.community.service.get;

import com.sungbok.community.dto.NotificationDTO;
import com.sungbok.community.dto.NotificationSettingsDTO;

import java.util.List;

/**
 * 알림 조회 서비스 인터페이스 (CQRS - Query)
 * 알림 목록, 읽지 않은 개수, 알림 설정 조회
 */
public interface GetNotificationService {

    /**
     * 사용자의 알림 목록을 조회합니다 (페이징)
     * org_id 자동 필터링, 생성일시 역순 정렬
     *
     * @param userId 사용자 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 알림 DTO 리스트
     */
    List<NotificationDTO> getNotifications(Long userId, int page, int size);

    /**
     * 사용자의 읽지 않은 알림 개수를 조회합니다
     * org_id 자동 필터링
     *
     * @param userId 사용자 ID
     * @return 읽지 않은 알림 개수
     */
    int getUnreadCount(Long userId);

    /**
     * 사용자의 알림 설정을 조회합니다
     * Valkey 캐시 우선, 미스 시 DB 조회
     *
     * @param userId 사용자 ID
     * @return 알림 설정 DTO
     */
    NotificationSettingsDTO getNotificationSettings(Long userId);
}
