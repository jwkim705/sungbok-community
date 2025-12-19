package com.sungbok.community.service;

import tools.jackson.databind.ObjectMapper;
import com.sungbok.community.dto.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis Queue 서비스
 * Redis List를 사용한 알림 이벤트 메시지 큐 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisQueueService {

    private static final String NOTIFICATION_QUEUE_KEY = "notification:queue";
    private static final long DEQUEUE_TIMEOUT_SECONDS = 5;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 알림 이벤트를 큐에 추가합니다.
     * Redis List의 오른쪽에 push (FIFO)
     *
     * @param event 알림 이벤트
     */
    public void enqueue(NotificationEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.opsForList().rightPush(NOTIFICATION_QUEUE_KEY, json);
            log.debug("알림 이벤트 큐에 추가: userId={}, type={}",
                    event.getUserId(), event.getNotificationType());
        } catch (Exception e) {
            log.error("알림 이벤트 큐 추가 실패: {}", e.getMessage(), e);
            throw new RuntimeException("알림 이벤트 큐 추가 실패", e);
        }
    }

    /**
     * 큐에서 알림 이벤트를 꺼냅니다.
     * Redis List의 왼쪽에서 블로킹 pop (FIFO)
     * 타임아웃: 5초
     *
     * @return 알림 이벤트 (큐가 비어있으면 null)
     */
    public NotificationEvent dequeue() {
        try {
            // BLPOP: 블로킹 방식으로 큐에서 꺼내기 (5초 타임아웃)
            String json = redisTemplate.opsForList()
                    .leftPop(NOTIFICATION_QUEUE_KEY, DEQUEUE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (json == null) {
                return null;  // 큐가 비어있음
            }

            NotificationEvent event = objectMapper.readValue(json, NotificationEvent.class);
            log.debug("알림 이벤트 큐에서 꺼냄: userId={}, type={}",
                    event.getUserId(), event.getNotificationType());
            return event;
        } catch (Exception e) {
            log.error("알림 이벤트 큐에서 꺼내기 실패: {}", e.getMessage(), e);
            return null;  // 에러 발생 시 null 반환 (Worker는 계속 실행)
        }
    }

    /**
     * 큐의 현재 길이를 조회합니다.
     * 모니터링용
     *
     * @return 큐에 있는 이벤트 개수
     */
    public long getQueueSize() {
        Long size = redisTemplate.opsForList().size(NOTIFICATION_QUEUE_KEY);
        return size != null ? size : 0;
    }
}
