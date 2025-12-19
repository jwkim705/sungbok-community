package com.sungbok.community.service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.sungbok.community.repository.NotificationSettingsRepository;
import com.sungbok.community.repository.PushTokensRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.generated.tables.pojos.NotificationSettings;
import org.jooq.generated.tables.pojos.PushTokens;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Valkey 알림 캐시 서비스
 * notification_settings와 push_tokens를 Valkey에 캐싱
 * DB 조회 부하 최소화 및 푸시 전송 성능 최적화
 * Valkey 9 Hash Field Expiration 사용 (기기별 독립적인 TTL)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValkeyNotificationCacheService {

    private static final String SETTINGS_KEY_PREFIX = "user:settings:";
    private static final String TOKENS_KEY_PREFIX = "user:tokens:";
    private static final long CACHE_TTL_DAYS = 7;
    private static final long PUSH_TOKEN_TTL_DAYS = 30;  // 푸시 토큰 30일 TTL

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final NotificationSettingsRepository notificationSettingsRepository;
    private final PushTokensRepository pushTokensRepository;

    /**
     * 사용자의 알림 설정을 조회합니다.
     * Valkey 캐시 우선, 미스 시 DB 조회 후 캐싱
     *
     * @param userId 사용자 ID
     * @return 알림 타입별 활성화 설정 Map (예: {"post_comment": true, "post_like": false})
     */
    public Map<String, Boolean> getNotificationPreferences(Long userId) {
        String key = SETTINGS_KEY_PREFIX + userId;
        String cached = redisTemplate.opsForValue().get(key);

        if (cached != null) {
            try {
                log.debug("알림 설정 캐시 히트: userId={}", userId);
                return objectMapper.readValue(cached, new TypeReference<Map<String, Boolean>>() {});
            } catch (Exception e) {
                log.warn("알림 설정 캐시 역직렬화 실패, DB 조회: userId={}", userId, e);
                redisTemplate.delete(key);  // 손상된 캐시 삭제
            }
        }

        // DB 조회 후 캐싱
        log.debug("알림 설정 캐시 미스, DB 조회: userId={}", userId);
        NotificationSettings settings = notificationSettingsRepository.fetchOrCreateByUserId(userId);

        try {
            // JSONB → Map 변환
            String jsonbValue = settings.getNotificationPreferences().data();
            Map<String, Boolean> preferences = objectMapper.readValue(
                    jsonbValue,
                    new TypeReference<Map<String, Boolean>>() {}
            );

            // Valkey 캐싱
            String json = objectMapper.writeValueAsString(preferences);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL_DAYS, TimeUnit.DAYS);

            return preferences;
        } catch (Exception e) {
            log.error("알림 설정 캐싱 실패: userId={}", userId, e);
            // 에러 발생 시 빈 Map 반환 (모든 알림 비활성화)
            return Map.of();
        }
    }

    /**
     * 사용자의 활성 Push Token 목록을 조회합니다.
     * Valkey 9 Hash Field Expiration 사용 (기기별 독립적인 TTL)
     * 하위 호환성을 위해 List도 함께 조회 (30일 후 자동 마이그레이션)
     *
     * @param userId 사용자 ID
     * @return Expo Push Token 리스트
     */
    public List<String> getActivePushTokens(Long userId) {
        String key = TOKENS_KEY_PREFIX + userId;
        List<String> tokens = new ArrayList<>();

        // 1. Hash에서 조회 (Valkey 9 방식)
        Map<Object, Object> hashTokens = redisTemplate.opsForHash().entries(key);
        if (!hashTokens.isEmpty()) {
            tokens.addAll(hashTokens.values().stream()
                .map(Object::toString)
                .collect(Collectors.toList()));
            log.debug("Push Token 캐시 히트 (Hash): userId={}, count={}", userId, tokens.size());
            return tokens;
        }

        // 2. 하위 호환성: List에서 조회 (구 방식, 30일 후 자동 만료)
        List<String> listTokens = redisTemplate.opsForList().range(key, 0, -1);
        if (listTokens != null && !listTokens.isEmpty()) {
            log.debug("Push Token 캐시 히트 (List, 구 방식): userId={}, count={}", userId, listTokens.size());
            return listTokens;
        }

        // 3. 캐시 미스: DB 조회 (Hash로 저장하지 않음, savePushToken 메서드에서 처리)
        log.debug("Push Token 캐시 미스, DB 조회: userId={}", userId);
        List<PushTokens> dbTokens = pushTokensRepository.fetchActiveByUserId(userId);
        return dbTokens.stream()
            .map(PushTokens::getExpoPushToken)
            .collect(Collectors.toList());
    }

    /**
     * Push Token 저장 (Valkey 9 Hash Field Expiration 사용)
     * 기기별 독립적인 30일 TTL 설정
     *
     * @param userId    사용자 ID
     * @param deviceId  기기 ID
     * @param token     Expo Push Token
     */
    public void savePushToken(Long userId, String deviceId, String token) {
        String key = TOKENS_KEY_PREFIX + userId;

        // Hash 필드에 저장
        redisTemplate.opsForHash().put(key, deviceId, token);

        // 해당 필드만 30일 TTL 설정 (Valkey 9 HEXPIRE)
        // HEXPIRE key seconds FIELDS numfields field [field ...]
        // HEXPIRE returns array of integers: 1 = TTL set, 0 = field doesn't exist, -2 = no expiration
        Long ttlSeconds = PUSH_TOKEN_TTL_DAYS * 24 * 60 * 60;
        redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            byte[] fieldBytes = deviceId.getBytes(StandardCharsets.UTF_8);
            Object result = connection.execute("HEXPIRE",
                keyBytes,
                ttlSeconds.toString().getBytes(StandardCharsets.UTF_8),
                "FIELDS".getBytes(StandardCharsets.UTF_8),
                "1".getBytes(StandardCharsets.UTF_8),  // numfields
                fieldBytes
            );

            // HEXPIRE returns List<Long> for multiple fields
            if (result instanceof java.util.List) {
                return ((java.util.List<?>) result).get(0);
            }
            return result;
        });

        log.debug("Push Token 저장 완료 (Hash): userId={}, deviceId={}", userId, deviceId);
    }

    /**
     * Push Token 삭제 (특정 기기만)
     * Valkey 9 Hash에서 특정 필드만 삭제
     *
     * @param userId    사용자 ID
     * @param deviceId  기기 ID
     */
    public void removePushToken(Long userId, String deviceId) {
        String key = TOKENS_KEY_PREFIX + userId;
        redisTemplate.opsForHash().delete(key, deviceId);
        log.debug("Push Token 삭제 완료: userId={}, deviceId={}", userId, deviceId);
    }

    /**
     * 사용자의 캐시를 무효화합니다.
     * 알림 설정 변경, Push Token 등록/삭제 시 호출
     *
     * @param userId 사용자 ID
     */
    public void invalidateUserCache(Long userId) {
        String settingsKey = SETTINGS_KEY_PREFIX + userId;
        String tokensKey = TOKENS_KEY_PREFIX + userId;

        redisTemplate.delete(settingsKey);
        redisTemplate.delete(tokensKey);

        log.debug("사용자 캐시 무효화: userId={}", userId);
    }

    /**
     * 로그인 시 사용자 캐시를 워밍업합니다 (선택적).
     * 미리 캐시를 로드하여 첫 푸시 전송 시 지연 방지
     *
     * @param userId 사용자 ID
     */
    public void warmupUserCache(Long userId) {
        log.debug("사용자 캐시 워밍업 시작: userId={}", userId);
        getNotificationPreferences(userId);
        getActivePushTokens(userId);
        log.debug("사용자 캐시 워밍업 완료: userId={}", userId);
    }
}
