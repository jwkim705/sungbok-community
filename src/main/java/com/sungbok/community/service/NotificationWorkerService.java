package com.sungbok.community.service;

import com.sungbok.community.dto.event.NotificationEvent;
import com.sungbok.community.repository.NotificationsRepository;
import com.sungbok.community.security.TenantContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.generated.tables.pojos.Notifications;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import tools.jackson.databind.ObjectMapper;

/**
 * 알림 Worker 서비스 (Gemini 패턴: Listener + Virtual Thread)
 *
 * <p>아키텍처:</p>
 * <ul>
 *   <li>Listener Thread (1-2개): Redis Queue 모니터링 (BLPOP)</li>
 *   <li>Virtual Thread (무제한): 메시지마다 생성, DB 저장 + 푸시 전송</li>
 * </ul>
 *
 * <p>처리 플로우:</p>
 * <ol>
 *   <li>Listener가 Redis Queue에서 이벤트 dequeue (블로킹 5초)</li>
 *   <li>메시지 받으면 즉시 Virtual Thread 생성</li>
 *   <li>Virtual Thread에서 TenantContext 설정 (멀티테넌시)</li>
 *   <li>알림 이력 DB 저장 + 푸시 알림 전송</li>
 *   <li>TenantContext 정리 (메모리 누수 방지)</li>
 * </ol>
 *
 * @since 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationWorkerService {

    private static final int LISTENER_THREAD_COUNT = 2;  // 큐 모니터링용 Listener

    private final RedisQueueService queueService;
    private final NotificationsRepository notificationsRepository;
    private final PushNotificationService pushNotificationService;
    private final MeterRegistry meterRegistry;

    private final List<Thread> listenerThreads = new ArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicInteger activeWorkers = new AtomicInteger(0);  // 모니터링용

    /**
     * 애플리케이션 시작 시 Listener 스레드 시작
     */
    @PostConstruct
    public void startListeners() {
        log.info("알림 Listener 시작: {} 스레드", LISTENER_THREAD_COUNT);
        running.set(true);

        // Gauge 메트릭 등록
        meterRegistry.gauge("notification.active.workers", activeWorkers);
        meterRegistry.gauge("notification.queue.size", queueService, RedisQueueService::getQueueSize);

        // 1-2개 Listener만 시작 (큐 모니터링)
        for (int i = 0; i < LISTENER_THREAD_COUNT; i++) {
            int listenerId = i + 1;
            Thread listener = Thread.ofPlatform()  // Platform Thread (큐 모니터링용)
                    .name("notification-listener-" + listenerId)
                    .start(() -> listenQueue(listenerId));

            listenerThreads.add(listener);
        }

        log.info("알림 Listener {} 개 시작 완료", listenerThreads.size());
    }

    /**
     * 애플리케이션 종료 시 Listener 스레드 우아한 종료
     */
    @PreDestroy
    public void stopListeners() {
        log.info("알림 Listener 종료 시작 (활성 워커: {})", activeWorkers.get());
        running.set(false);

        // Listener 인터럽트
        listenerThreads.forEach(Thread::interrupt);

        // 5초 대기 (BLPOP 타임아웃)
        for (Thread listener : listenerThreads) {
            try {
                listener.join(5000);
            } catch (InterruptedException e) {
                log.warn("Listener 종료 대기 중 인터럽트: {}", e.getMessage());
                Thread.currentThread().interrupt();
            }
        }

        log.info("알림 Listener 종료 완료 (남은 활성 워커: {})", activeWorkers.get());
    }

    /**
     * Redis Queue를 모니터링하는 Listener 루프
     * 메시지 받으면 즉시 Virtual Thread 생성하여 처리
     *
     * @param listenerId Listener 식별자 (로그용)
     */
    private void listenQueue(int listenerId) {
        log.info("Listener-{} 시작 (큐 모니터링)", listenerId);

        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                // Redis Queue에서 이벤트 꺼내기 (블로킹 5초)
                NotificationEvent event = queueService.dequeue();

                if (event == null) {
                    continue;  // 타임아웃, 다시 대기
                }

                long queueSize = queueService.getQueueSize();
                log.debug("Listener-{} 메시지 수신: userId={}, type={}, 큐크기={}, 활성워커={}",
                        listenerId, event.getUserId(), event.getNotificationType(),
                        queueSize, activeWorkers.get());

                // 메시지마다 Virtual Thread 생성 (즉시 처리)
                activeWorkers.incrementAndGet();
                Thread.ofVirtual()
                        .name("notification-worker-" + event.getUserId())
                        .start(() -> {
                            try {
                                processNotification(event);
                            } finally {
                                activeWorkers.decrementAndGet();
                            }
                        });

            } catch (Exception e) {
                log.error("Listener-{} 메시지 처리 중 오류: {}", listenerId, e.getMessage(), e);
                // 에러 발생 시에도 Listener는 계속 실행
            }
        }

        log.info("Listener-{} 종료", listenerId);
    }

    /**
     * Virtual Thread에서 실행되는 알림 처리 로직
     * DB 저장 + Expo 푸시 전송 (I/O 블로킹 작업)
     * Micrometer 메트릭 수집: 처리 시간, 성공/실패 카운트
     *
     * @param event 알림 이벤트
     */
    private void processNotification(NotificationEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        long startTime = System.currentTimeMillis();

        // TenantContext 설정 (멀티테넌시)
        TenantContext.setOrgId(event.getOrgId());
        try {
            handleNotification(event);  // 기존 로직 그대로 사용

            // 성공 메트릭 기록
            meterRegistry.counter("notification.processed",
                    "type", event.getNotificationType().name(),
                    "status", "success").increment();

            log.debug("워커 처리 완료: userId={}, 처리시간={}ms, 활성워커={}",
                    event.getUserId(),
                    System.currentTimeMillis() - startTime,
                    activeWorkers.get());
        } catch (Exception e) {
            // 실패 메트릭 기록
            meterRegistry.counter("notification.processed",
                    "type", event.getNotificationType().name(),
                    "status", "failure").increment();

            log.error("워커 처리 실패: userId={}, type={}, error={}",
                    event.getUserId(), event.getNotificationType(), e.getMessage(), e);
        } finally {
            // 처리 시간 메트릭 기록
            sample.stop(meterRegistry.timer("notification.processing.time",
                    "type", event.getNotificationType().name()));

            TenantContext.clear();  // 메모리 누수 방지
        }
    }

    /**
     * 알림 이벤트를 처리합니다.
     * 1. 알림 이력 DB 저장 (푸시 발송 여부와 무관)
     * 2. 푸시 알림 전송 (Valkey 캐시 사용, 설정 확인 후)
     *
     * @param event 알림 이벤트
     */
    @Transactional
    public void handleNotification(NotificationEvent event) {
        // 1. 알림 이력 먼저 DB에 저장 (푸시 발송 여부와 무관)
        Notifications notification = new Notifications();
        notification.setUserId(event.getUserId());
        notification.setNotificationType(event.getNotificationType().name());
        notification.setTitle(event.getTitle());
        notification.setBody(event.getBody());
        notification.setRelatedEntityType(event.getRelatedEntityType());
        notification.setRelatedEntityId(event.getRelatedEntityId());
        notification.setIsRead(false);  // 초기값: 안 읽음
        notification.setPushSent(false);  // 초기값: 푸시 미전송
        notification.setCreatedAt(LocalDateTime.now());
        notification.setModifiedAt(LocalDateTime.now());

        // metadata 설정 (data를 JSONB로 변환)
        if (event.getData() != null && !event.getData().isEmpty()) {
            try {
                String metadataJson = new ObjectMapper().writeValueAsString(event.getData());
                notification.setMetadata(org.jooq.JSONB.valueOf(metadataJson));
            } catch (Exception e) {
                log.warn("메타데이터 직렬화 실패: {}", e.getMessage());
            }
        }

        Notifications saved = notificationsRepository.insert(notification);

        log.debug("알림 이력 DB 저장: notificationId={}, userId={}, type={}",
                saved.getNotificationId(), event.getUserId(), event.getNotificationType());

        // 2. 푸시 알림 전송 (설정 확인 후, Valkey 캐시 사용)
        pushNotificationService.sendPushNotification(
                event.getUserId(),
                event,
                saved.getNotificationId()
        );
    }
}
