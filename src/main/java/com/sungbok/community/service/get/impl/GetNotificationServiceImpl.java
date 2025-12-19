package com.sungbok.community.service.get.impl;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.sungbok.community.dto.NotificationDTO;
import com.sungbok.community.dto.NotificationSettingsDTO;
import com.sungbok.community.enums.NotificationType;
import com.sungbok.community.repository.NotificationSettingsRepository;
import com.sungbok.community.repository.NotificationsRepository;
import com.sungbok.community.service.ValkeyNotificationCacheService;
import com.sungbok.community.service.get.GetNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.generated.tables.pojos.NotificationSettings;
import org.jooq.generated.tables.pojos.Notifications;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 알림 조회 서비스 구현체 (CQRS - Query)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetNotificationServiceImpl implements GetNotificationService {

    private final NotificationsRepository notificationsRepository;
    private final NotificationSettingsRepository notificationSettingsRepository;
    private final ValkeyNotificationCacheService valkeyCache;
    private final ObjectMapper objectMapper;

    @Override
    public List<NotificationDTO> getNotifications(Long userId, int page, int size) {
        int offset = page * size;
        List<Notifications> notifications = notificationsRepository.fetchByUserId(userId, size, offset);

        return notifications.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public int getUnreadCount(Long userId) {
        return notificationsRepository.countUnreadByUserId(userId);
    }

    @Override
    public NotificationSettingsDTO getNotificationSettings(Long userId) {
        // Valkey 캐시 우선 조회
        Map<String, Boolean> preferences = valkeyCache.getNotificationPreferences(userId);

        // DB에서 enablePushNotifications 조회 (캐시에는 preferences만 있음)
        NotificationSettings settings = notificationSettingsRepository.fetchOrCreateByUserId(userId);

        return NotificationSettingsDTO.builder()
                .notificationPreferences(preferences)
                .enablePushNotifications(settings.getEnablePushNotifications())
                .build();
    }

    /**
     * Notifications POJO를 NotificationDTO로 변환합니다
     *
     * @param notification Notifications POJO
     * @return NotificationDTO
     */
    private NotificationDTO toDTO(Notifications notification) {
        Map<String, Object> metadata = null;

        // JSONB → Map 변환
        if (notification.getMetadata() != null) {
            try {
                String jsonString = notification.getMetadata().data();
                metadata = objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.warn("메타데이터 역직렬화 실패: notificationId={}", notification.getNotificationId(), e);
            }
        }

        return NotificationDTO.builder()
                .notificationId(notification.getNotificationId())
                .notificationType(NotificationType.valueOf(notification.getNotificationType()))
                .title(notification.getTitle())
                .body(notification.getBody())
                .relatedEntityType(notification.getRelatedEntityType())
                .relatedEntityId(notification.getRelatedEntityId())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .metadata(metadata)
                .build();
    }
}
