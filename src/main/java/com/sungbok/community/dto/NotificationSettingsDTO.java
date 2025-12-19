package com.sungbok.community.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * 알림 설정 DTO
 * GET/PUT /api/notifications/settings 엔드포인트에서 사용
 * JSONB notification_preferences 컬럼을 Map으로 매핑
 *
 * @since 0.0.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettingsDTO {

    /**
     * 알림 타입별 활성화 설정 (JSONB 매핑)
     * 예: {"post_comment": true, "post_like": false, "membership_approved": true}
     *
     * 지원 타입:
     * - post_comment: 게시글 댓글
     * - post_like: 게시글 좋아요
     * - membership_approved: 멤버십 승인
     * - membership_rejected: 멤버십 거절
     * - admin_announcement: 관리자 공지
     */
    @NotNull(message = "알림 설정은 필수입니다")
    private Map<String, Boolean> notificationPreferences;

    /**
     * 푸시 알림 마스터 스위치
     * false일 경우 모든 푸시 알림 비활성화
     */
    @NotNull(message = "푸시 알림 활성화 여부는 필수입니다")
    private Boolean enablePushNotifications;
}
