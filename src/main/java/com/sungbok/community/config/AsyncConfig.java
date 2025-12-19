package com.sungbok.community.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 비동기 처리 설정
 * @Async 어노테이션 활성화
 *
 * 사용 예시:
 * - AuditLogService.log() - 비동기 감사 로그 기록
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // @Async 활성화
    // 별도 설정 없이 기본 SimpleAsyncTaskExecutor 사용
}
