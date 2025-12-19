package com.sungbok.community.dto.event;

import com.sungbok.community.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 알림 이벤트 DTO
 * Redis Queue에 전송되는 알림 이벤트 메시지
 * NotificationWorkerService에서 소비하여 알림 생성 및 푸시 전송
 *
 * @since 0.0.1
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 조직 ID (멀티테넌시)
     */
    private Long orgId;

    /**
     * 수신자 사용자 ID
     */
    private Long userId;

    /**
     * 알림 타입
     * 예: POST_COMMENT, POST_LIKE, MEMBERSHIP_APPROVED, MEMBERSHIP_REJECTED, ADMIN_ANNOUNCEMENT
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
     * 예: post, comment, membership, announcement
     */
    private String relatedEntityType;

    /**
     * 관련 엔티티 ID
     */
    private Long relatedEntityId;

    /**
     * 추가 데이터 (딥링크 등에 사용)
     * 예: {"postId": 123, "commentId": 456}
     */
    private Map<String, Object> data;
}
