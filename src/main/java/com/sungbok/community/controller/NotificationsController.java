package com.sungbok.community.controller;

import com.sungbok.community.dto.NotificationDTO;
import com.sungbok.community.dto.NotificationSettingsDTO;
import com.sungbok.community.dto.PushTokenRequest;
import com.sungbok.community.dto.UnreadCountDTO;
import com.sungbok.community.security.model.PrincipalDetails;
import com.sungbok.community.service.change.ChangeNotificationService;
import com.sungbok.community.service.get.GetNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Notifications API 컨트롤러
 * 알림 목록, 읽음 처리, Push Token 관리, 알림 설정
 *
 * 모든 엔드포인트는 인증 필수 (JWT)
 *
 * @since 0.0.1
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationsController {

    private final GetNotificationService getNotificationService;
    private final ChangeNotificationService changeNotificationService;

    /**
     * GET /api/notifications
     * 사용자의 알림 목록 조회 (페이징)
     *
     * @param userDetails 인증된 사용자 정보 (JWT)
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @return 알림 DTO 리스트
     */
    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getNotifications(
            @AuthenticationPrincipal PrincipalDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = userDetails.getUser().getUserId();
        List<NotificationDTO> notifications = getNotificationService.getNotifications(userId, page, size);

        return ResponseEntity.ok(notifications);
    }

    /**
     * GET /api/notifications/unread-count
     * 읽지 않은 알림 개수 조회
     *
     * @param userDetails 인증된 사용자 정보 (JWT)
     * @return 읽지 않은 알림 개수
     */
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountDTO> getUnreadCount(
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long userId = userDetails.getUser().getUserId();
        int count = getNotificationService.getUnreadCount(userId);

        return ResponseEntity.ok(new UnreadCountDTO(count));
    }

    /**
     * PUT /api/notifications/{notificationId}/read
     * 알림 읽음 처리
     *
     * @param userDetails 인증된 사용자 정보 (JWT)
     * @param notificationId 알림 ID
     * @return 성공 응답
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal PrincipalDetails userDetails,
            @PathVariable Long notificationId
    ) {
        Long userId = userDetails.getUser().getUserId();
        changeNotificationService.markAsRead(notificationId, userId);

        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/notifications/read-all
     * 모든 알림 읽음 처리
     *
     * @param userDetails 인증된 사용자 정보 (JWT)
     * @return 성공 응답
     */
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long userId = userDetails.getUser().getUserId();
        changeNotificationService.markAllAsRead(userId);

        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/notifications/push-tokens
     * Push Token 등록
     *
     * @param userDetails 인증된 사용자 정보 (JWT)
     * @param request Push Token 등록 요청
     * @return 성공 응답
     */
    @PostMapping("/push-tokens")
    public ResponseEntity<Void> registerPushToken(
            @AuthenticationPrincipal PrincipalDetails userDetails,
            @Valid @RequestBody PushTokenRequest request
    ) {
        Long userId = userDetails.getUser().getUserId();
        changeNotificationService.registerPushToken(userId, request);

        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/notifications/push-tokens
     * Push Token 삭제 (Valkey 9 Hash Field Expiration - 특정 기기만 삭제)
     *
     * @param userDetails 인증된 사용자 정보 (JWT)
     * @param deviceId    삭제할 기기 ID (쿼리 파라미터)
     * @return 성공 응답
     */
    @DeleteMapping("/push-tokens")
    public ResponseEntity<Void> unregisterPushToken(
            @AuthenticationPrincipal PrincipalDetails userDetails,
            @RequestParam String deviceId
    ) {
        Long userId = userDetails.getUser().getUserId();
        changeNotificationService.unregisterPushToken(userId, deviceId);

        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/notifications/settings
     * 알림 설정 조회
     *
     * @param userDetails 인증된 사용자 정보 (JWT)
     * @return 알림 설정 DTO
     */
    @GetMapping("/settings")
    public ResponseEntity<NotificationSettingsDTO> getNotificationSettings(
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long userId = userDetails.getUser().getUserId();
        NotificationSettingsDTO settings = getNotificationService.getNotificationSettings(userId);

        return ResponseEntity.ok(settings);
    }

    /**
     * PUT /api/notifications/settings
     * 알림 설정 변경
     *
     * @param userDetails 인증된 사용자 정보 (JWT)
     * @param settingsDTO 알림 설정 DTO
     * @return 성공 응답
     */
    @PutMapping("/settings")
    public ResponseEntity<Void> updateNotificationSettings(
            @AuthenticationPrincipal PrincipalDetails userDetails,
            @Valid @RequestBody NotificationSettingsDTO settingsDTO
    ) {
        Long userId = userDetails.getUser().getUserId();
        changeNotificationService.updateNotificationSettings(userId, settingsDTO);

        return ResponseEntity.noContent().build();
    }
}
