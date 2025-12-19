package com.sungbok.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jooq.JSONB;

import java.time.LocalDateTime;

/**
 * 감사 로그 DTO
 * 시스템 변경 이력 조회 시 사용 (관리자 전용)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDTO {

    /**
     * 감사 로그 ID
     */
    private Long id;

    /**
     * 조직 ID
     * NULL이면 플랫폼 레벨 작업
     */
    private Long orgId;

    /**
     * 사용자 ID
     * 작업을 수행한 사용자
     */
    private Long userId;

    /**
     * 작업 타입
     * 예: USER_LOGIN, POST_DELETE, ROLE_CHANGE, MEMBERSHIP_APPROVE
     */
    private String action;

    /**
     * 리소스 타입
     * 예: post, membership, role
     */
    private String resourceType;

    /**
     * 리소스 ID
     * 변경된 리소스의 ID
     */
    private Long resourceId;

    /**
     * 변경 전 값
     * JSONB 형식
     */
    private JSONB oldValue;

    /**
     * 변경 후 값
     * JSONB 형식
     */
    private JSONB newValue;

    /**
     * 작업 시 IP 주소
     */
    private String ipAddress;

    /**
     * User Agent
     * 클라이언트 정보
     */
    private String userAgent;

    /**
     * 작업 일시
     */
    private LocalDateTime createdAt;
}
