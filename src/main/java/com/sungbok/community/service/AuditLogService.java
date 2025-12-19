package com.sungbok.community.service;

import com.sungbok.community.dto.AuditLogDTO;
import org.jooq.JSONB;

import java.util.List;

/**
 * 감사 로그 서비스
 * 시스템 내 중요 작업의 감사 로그 기록 및 조회
 */
public interface AuditLogService {

    /**
     * 감사 로그 비동기 기록
     * 비즈니스 로직 성능에 영향 없도록 @Async 처리
     *
     * @param orgId 조직 ID (NULL이면 플랫폼 레벨)
     * @param userId 사용자 ID
     * @param action 작업 타입 (예: USER_LOGIN, POST_DELETE)
     * @param resourceType 리소스 타입 (예: post, user)
     * @param resourceId 리소스 ID
     * @param oldValue 이전 값 (JSON)
     * @param newValue 새 값 (JSON)
     * @param ipAddress 요청 IP
     * @param userAgent 요청 User-Agent
     */
    void log(Long orgId, Long userId, String action, String resourceType,
             Long resourceId, JSONB oldValue, JSONB newValue,
             String ipAddress, String userAgent);

    /**
     * 조직별 감사 로그 조회 (관리자 전용)
     *
     * @param orgId 조직 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 감사 로그 리스트
     */
    List<AuditLogDTO> getAuditLogs(Long orgId, int page, int size);

    /**
     * 특정 리소스의 변경 이력 조회 (관리자 전용)
     *
     * @param orgId 조직 ID
     * @param resourceType 리소스 타입
     * @param resourceId 리소스 ID
     * @return 해당 리소스의 감사 로그 리스트
     */
    List<AuditLogDTO> getResourceHistory(Long orgId, String resourceType, Long resourceId);
}
