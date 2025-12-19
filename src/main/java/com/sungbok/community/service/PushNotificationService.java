package com.sungbok.community.service;

import com.sungbok.community.dto.PushNotificationRequest;
import com.sungbok.community.dto.event.NotificationEvent;
import org.jooq.generated.enums.PushStatus;
import com.sungbok.community.repository.NotificationsRepository;
import com.sungbok.community.repository.PushTokensRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Expo Push Notification 서비스
 * Expo Push Notification API를 호출하여 모바일 앱에 푸시 알림 전송
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private static final String EXPO_PUSH_API_URL = "https://exp.host/--/api/v2/push/send";
    private static final String PRIORITY_DEFAULT = "default";
    private static final String SOUND_DEFAULT = "default";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ValkeyNotificationCacheService valkeyCache;
    private final NotificationsRepository notificationsRepository;
    private final PushTokensRepository pushTokensRepository;

    /**
     * 사용자에게 푸시 알림을 전송합니다.
     * Valkey 캐시에서 알림 설정 및 토큰 조회 후 Expo API 호출
     *
     * @param userId 수신자 사용자 ID
     * @param event 알림 이벤트
     * @param notificationId DB에 저장된 알림 ID (푸시 상태 업데이트용)
     */
    public void sendPushNotification(Long userId, NotificationEvent event, Long notificationId) {
        try {
            // 1. Valkey 캐시에서 알림 설정 조회
            Map<String, Boolean> preferences = valkeyCache.getNotificationPreferences(userId);

            // 푸시 알림 마스터 스위치 확인 (enablePushNotifications는 별도 조회 필요 시 추가)
            // 현재는 preferences만으로 판단 (타입별 설정)

            // 2. 알림 타입 활성화 여부 확인
            String notificationType = event.getNotificationType().name().toLowerCase();
            String preferenceKey = camelToSnakeCase(notificationType);  // POST_COMMENT → post_comment

            if (!preferences.getOrDefault(preferenceKey, true)) {
                log.debug("알림 타입 비활성화, 푸시 전송 스킵: userId={}, type={}",
                        userId, notificationType);
                return;
            }

            // 3. Valkey 캐시에서 Push Token 조회
            List<String> tokens = valkeyCache.getActivePushTokens(userId);

            if (tokens.isEmpty()) {
                log.debug("활성 Push Token 없음, 푸시 전송 스킵: userId={}", userId);
                return;
            }

            // 4. Expo API 호출
            PushNotificationRequest request = PushNotificationRequest.builder()
                    .to(tokens)
                    .title(event.getTitle())
                    .body(event.getBody())
                    .data(event.getData())
                    .priority(PRIORITY_DEFAULT)
                    .sound(SOUND_DEFAULT)
                    .build();

            sendToExpo(request, notificationId);

        } catch (Exception e) {
            log.error("푸시 알림 전송 실패: userId={}, notificationId={}, error={}",
                    userId, notificationId, e.getMessage(), e);

            // 에러 상태 업데이트
            if (notificationId != null) {
                notificationsRepository.updatePushStatus(notificationId, PushStatus.ERROR, e.getMessage());
            }
        }
    }

    /**
     * Expo Push Notification API를 호출합니다.
     * 최대 3회 재시도 (지수 백오프: 1초 → 2초 → 4초)
     * HTTP 429, 5xx 에러 시 재시도
     *
     * @param request Expo Push 요청 DTO
     * @param notificationId 알림 ID (상태 업데이트용)
     */
    @Retryable(
        includes = {RestClientException.class, ResourceAccessException.class},
        delay = 1000,
        multiplier = 2.0,
        maxDelay = 5000
    )
    private void sendToExpo(PushNotificationRequest request, Long notificationId) {
        try {
            // HTTP 요청 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");

            // 요청 본문 생성
            HttpEntity<PushNotificationRequest> entity = new HttpEntity<>(request, headers);

            // Expo API 호출
            log.debug("Expo API 호출: tokens={}, title={}", request.getTo(), request.getTitle());
            ResponseEntity<String> response = restTemplate.exchange(
                    EXPO_PUSH_API_URL,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            // HTTP 429 Too Many Requests → 재시도
            if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                throw new RestClientException("Rate limit exceeded");
            }

            // 응답 처리
            if (response.getStatusCode() == HttpStatus.OK) {
                handleExpoResponse(response.getBody(), request.getTo(), notificationId);
            } else {
                log.error("Expo API 호출 실패: status={}, body={}",
                        response.getStatusCode(), response.getBody());
                notificationsRepository.updatePushStatus(
                        notificationId,
                        PushStatus.ERROR,
                        "HTTP " + response.getStatusCode()
                );
            }

        } catch (HttpClientErrorException e) {
            // 4xx 에러는 재시도하지 않고 즉시 종료
            if (!e.getStatusCode().equals(HttpStatus.TOO_MANY_REQUESTS)) {
                log.error("Expo API 클라이언트 오류 (재시도 안 함): {}", e.getMessage());
                notificationsRepository.updatePushStatus(notificationId, PushStatus.ERROR, e.getMessage());
                return;
            }
            // 429 에러는 재시도
            throw new RestClientException("Rate limit", e);
        } catch (RestClientException e) {
            // 5xx 에러, 네트워크 에러 → 재시도
            log.warn("Expo API 호출 실패 (재시도 예정): {}", e.getMessage());
            throw e;  // Spring Retry가 재시도
        } catch (Exception e) {
            log.error("Expo API 호출 예외: {}", e.getMessage(), e);
            notificationsRepository.updatePushStatus(notificationId, PushStatus.ERROR, e.getMessage());
        }
    }


    /**
     * Expo API 응답을 처리합니다.
     * 성공/에러 상태를 DB에 업데이트하고, 유효하지 않은 토큰은 비활성화
     *
     * @param responseBody Expo API 응답 본문 (JSON)
     * @param tokens 전송한 토큰 목록
     * @param notificationId 알림 ID
     */
    private void handleExpoResponse(String responseBody, List<String> tokens, Long notificationId) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode dataNode = root.get("data");

            if (dataNode == null || !dataNode.isArray()) {
                log.warn("Expo API 응답 형식 오류: {}", responseBody);
                notificationsRepository.updatePushStatus(notificationId, PushStatus.ERROR, "Invalid response format");
                return;
            }

            // 각 토큰별 응답 처리
            for (int i = 0; i < dataNode.size(); i++) {
                JsonNode result = dataNode.get(i);
                String status = result.get("status").asString();
                String token = tokens.get(i);

                if ("ok".equals(status)) {
                    log.debug("푸시 전송 성공: token={}", maskToken(token));
                    notificationsRepository.updatePushStatus(notificationId, PushStatus.OK, null);
                } else if ("error".equals(status)) {
                    JsonNode detailsNode = result.get("details");
                    String error = detailsNode != null ? detailsNode.get("error").asString() : "unknown";

                    log.warn("푸시 전송 실패: token={}, error={}", maskToken(token), error);

                    // DeviceNotRegistered: 토큰 비활성화
                    if ("DeviceNotRegistered".equals(error)) {
                        pushTokensRepository.deactivateToken(token);
                        log.info("유효하지 않은 토큰 비활성화: token={}", maskToken(token));
                    }

                    notificationsRepository.updatePushStatus(notificationId, PushStatus.ERROR, error);
                }
            }

        } catch (Exception e) {
            log.error("Expo 응답 처리 실패: {}", e.getMessage(), e);
            notificationsRepository.updatePushStatus(notificationId, PushStatus.ERROR, "Response parsing error");
        }
    }

    /**
     * camelCase를 snake_case로 변환합니다.
     * 예: POST_COMMENT → post_comment, postComment → post_comment
     *
     * @param camelCase camelCase 문자열
     * @return snake_case 문자열
     */
    private String camelToSnakeCase(String camelCase) {
        return camelCase
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .replaceAll("([A-Z])([A-Z][a-z])", "$1_$2")
                .toLowerCase();
    }

    /**
     * 토큰의 일부를 마스킹합니다 (로그 출력용).
     * 예: ExponentPushToken[xxxxxx...xxxxxx] → ExponentPushToken[xxxx...xxxx]
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
