package com.sungbok.community.service.change.impl;

import tools.jackson.databind.ObjectMapper;
import com.sungbok.community.dto.NotificationSettingsDTO;
import com.sungbok.community.dto.PushTokenRequest;
import com.sungbok.community.repository.NotificationSettingsRepository;
import com.sungbok.community.repository.NotificationsRepository;
import com.sungbok.community.repository.PushTokensRepository;
import com.sungbok.community.service.ValkeyNotificationCacheService;
import com.sungbok.community.service.change.ChangeNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.JSONB;
import org.jooq.generated.tables.pojos.NotificationSettings;
import org.jooq.generated.tables.pojos.PushTokens;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 알림 변경 서비스 구현체 (CQRS - Command)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChangeNotificationServiceImpl implements ChangeNotificationService {

    private final NotificationsRepository notificationsRepository;
    private final PushTokensRepository pushTokensRepository;
    private final NotificationSettingsRepository notificationSettingsRepository;
    private final ValkeyNotificationCacheService valkeyCache;
    private final ObjectMapper objectMapper;

    @Override
    public void markAsRead(Long notificationId, Long userId) {
        int affected = notificationsRepository.markAsRead(notificationId, userId);

        if (affected == 0) {
            log.warn("알림 읽음 처리 실패: notificationId={}, userId={}", notificationId, userId);
            throw new IllegalArgumentException("알림을 찾을 수 없거나 권한이 없습니다");
        }

        log.debug("알림 읽음 처리: notificationId={}, userId={}", notificationId, userId);
    }

    @Override
    public void markAllAsRead(Long userId) {
        int affected = notificationsRepository.markAllAsRead(userId);
        log.debug("모든 알림 읽음 처리: userId={}, count={}", userId, affected);
    }

    @Override
    public void registerPushToken(Long userId, PushTokenRequest request) {
        PushTokens pushToken = new PushTokens();
        pushToken.setUserId(userId);
        pushToken.setExpoPushToken(request.getExpoPushToken());
        pushToken.setDeviceType(request.getDeviceType());
        pushToken.setDeviceName(request.getDeviceName());
        pushToken.setAppVersion(request.getAppVersion());
        pushToken.setCreatedAt(LocalDateTime.now());
        pushToken.setModifiedAt(LocalDateTime.now());

        pushTokensRepository.upsert(pushToken);

        // Valkey Hash에 즉시 캐싱 (Valkey 9 Hash Field Expiration, 30일 TTL)
        valkeyCache.savePushToken(userId, request.getDeviceId(), request.getExpoPushToken());

        log.debug("Push Token 등록: userId={}, deviceId={}, token={}",
            userId, request.getDeviceId(), maskToken(request.getExpoPushToken()));
    }

    @Override
    public void unregisterPushToken(Long userId, String deviceId) {
        // Valkey Hash에서 특정 기기만 삭제 (Valkey 9)
        valkeyCache.removePushToken(userId, deviceId);

        // 전체 캐시 무효화 (DB와 캐시 일관성 유지)
        valkeyCache.invalidateUserCache(userId);

        log.debug("Push Token 삭제: userId={}, deviceId={}", userId, deviceId);
    }

    @Override
    public void updateNotificationSettings(Long userId, NotificationSettingsDTO settingsDTO) {
        try {
            // Map → JSONB 변환
            String jsonString = objectMapper.writeValueAsString(settingsDTO.getNotificationPreferences());
            JSONB jsonb = JSONB.valueOf(jsonString);

            // 기존 설정 조회 또는 생성
            NotificationSettings settings = notificationSettingsRepository.fetchOrCreateByUserId(userId);
            settings.setNotificationPreferences(jsonb);
            settings.setEnablePushNotifications(settingsDTO.getEnablePushNotifications());
            settings.setModifiedAt(LocalDateTime.now());

            notificationSettingsRepository.update(settings);

            // Valkey 캐시 무효화
            valkeyCache.invalidateUserCache(userId);

            log.debug("알림 설정 변경: userId={}, preferences={}, enablePush={}",
                    userId, settingsDTO.getNotificationPreferences(), settingsDTO.getEnablePushNotifications());

        } catch (Exception e) {
            log.error("알림 설정 변경 실패: userId={}", userId, e);
            throw new RuntimeException("알림 설정 변경에 실패했습니다", e);
        }
    }

    /**
     * 토큰의 일부를 마스킹합니다 (로그 출력용)
     *
     * @param token Expo Push Token
     * @return 마스킹된 토큰
     */
    private String maskToken(String token) {
        if (token.length() > 30) {
            return token.substring(0, 25) + "..." + token.substring(token.length() - 5);
        }
        return token;
    }
}
