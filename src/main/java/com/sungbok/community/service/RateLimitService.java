package com.sungbok.community.service;

/**
 * Rate Limiting 서비스
 * Valkey(Redis) 기반 요청 제한 (Sliding Window 1분)
 */
public interface RateLimitService {

    /**
     * 요청 허용 여부 확인
     * Valkey INCR + EXPIRE를 Atomic하게 실행
     *
     * @param identifier 사용자 식별자 (user:{userId} 또는 ip:{ipAddress})
     * @param endpoint 엔드포인트 경로
     * @return true: 허용, false: 제한 초과
     */
    boolean isAllowed(String identifier, String endpoint);

    /**
     * 남은 요청 수 조회
     *
     * @param identifier 사용자 식별자
     * @param endpoint 엔드포인트 경로
     * @return 남은 요청 수 (음수면 제한 초과)
     */
    int getRemainingRequests(String identifier, String endpoint);
}
