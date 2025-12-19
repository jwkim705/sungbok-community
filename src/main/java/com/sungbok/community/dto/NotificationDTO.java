package com.sungbok.community.dto;

import com.sungbok.community.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 알림 응답 DTO
 * GET /api/notifications 엔드포인트에서 사용
 * 사용자 알림 목록 조회 시 반환
 *
 * @since 0.0.1
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {

    /**
     * 알림 ID
     */
    private Long notificationId;

    /**
     * 알림 타입
     * 예: POST_COMMENT, POST_LIKE, MEMBERSHIP_APPROVED
     */
    private NotificationType notificationType;

    /**
     * 알림 제목
     */
    private String title;

    /**
     * 알림 본문
     */
    private String body;

    /**
     * 관련 엔티티 타입
     * 예: post, comment, membership
     */
    private String relatedEntityType;

    /**
     * 관련 엔티티 ID
     */
    private Long relatedEntityId;

    /**
     * 읽음 여부
     */
    private Boolean isRead;

    /**
     * 읽은 시각
     */
    private LocalDateTime readAt;

    /**
     * 알림 생성 시각
     */
    private LocalDateTime createdAt;

    /**
     * 추가 메타데이터 (JSONB)
     * 예: {"postTitle": "안녕하세요", "authorName": "홍길동"}
     */
    private Map<String, Object> metadata;
}
