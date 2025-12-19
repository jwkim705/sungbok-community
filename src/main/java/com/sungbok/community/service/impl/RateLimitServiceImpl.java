package com.sungbok.community.service.impl;

import com.sungbok.community.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Rate Limiting 서비스 구현체
 * Fail-open 전략: Valkey 장애 시 요청 허용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

    private final RedisTemplate<String, Object> valkeyTemplate;

    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(1);

    @Override
    public boolean isAllowed(String identifier, String endpoint) {
        try {
            String key = buildKey(identifier, endpoint);
            Long currentCount = valkeyTemplate.opsForValue().increment(key);

            if (currentCount == null) {
                log.warn("Rate limit 체크 실패: increment가 null 반환, key={}", key);
                return true; // Fail-open
            }

            // 첫 요청이면 TTL 설정
            if (currentCount == 1) {
                valkeyTemplate.expire(key, WINDOW_DURATION);
            }

            boolean allowed = currentCount <= MAX_REQUESTS_PER_MINUTE;
            if (!allowed) {
                log.warn("Rate limit 초과: identifier={}, endpoint={}, count={}",
                         identifier, endpoint, currentCount);
            }
            return allowed;

        } catch (Exception e) {
            // Fail-open: Valkey 장애 시 요청 허용
            log.error("Rate limit 체크 실패, 요청 허용: identifier={}, error={}",
                      identifier, e.getMessage());
            return true;
        }
    }

    @Override
    public int getRemainingRequests(String identifier, String endpoint) {
        try {
            String key = buildKey(identifier, endpoint);
            String countStr = (String) valkeyTemplate.opsForValue().get(key);
            int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;
            return MAX_REQUESTS_PER_MINUTE - currentCount;
        } catch (Exception e) {
            log.error("남은 요청 수 조회 실패: identifier={}, error={}",
                      identifier, e.getMessage());
            return MAX_REQUESTS_PER_MINUTE; // Fail-open
        }
    }

    /**
     * Rate limit 키 생성
     * 패턴: ratelimit:{identifier}:{endpoint}
     */
    private String buildKey(String identifier, String endpoint) {
        return "ratelimit:" + identifier + ":" + endpoint;
    }
}
