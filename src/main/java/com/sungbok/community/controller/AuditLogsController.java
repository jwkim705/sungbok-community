package com.sungbok.community.controller;

import com.sungbok.community.dto.AuditLogDTO;
import com.sungbok.community.security.TenantContext;
import com.sungbok.community.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 감사 로그 컨트롤러
 * 관리자 전용 - 시스템 감사 로그 조회
 */
@Slf4j
@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditLogsController {

    private final AuditLogService auditLogService;

    /**
     * 감사 로그 조회 (관리자 전용)
     * 페이징 지원
     *
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 50)
     * @return 감사 로그 리스트
     */
    @GetMapping
    @PreAuthorize("@permissionChecker.hasPermission(authentication, 'audit_logs', 'read')")
    public ResponseEntity<List<AuditLogDTO>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Long orgId = TenantContext.getRequiredOrgId();
        log.info("감사 로그 조회: orgId={}, page={}, size={}", orgId, page, size);

        List<AuditLogDTO> logs = auditLogService.getAuditLogs(orgId, page, size);
        return ResponseEntity.ok(logs);
    }

    /**
     * 특정 리소스의 변경 이력 조회 (관리자 전용)
     *
     * @param resourceType 리소스 타입 (예: post, user)
     * @param resourceId 리소스 ID
     * @return 해당 리소스의 감사 로그 리스트
     */
    @GetMapping("/resource")
    @PreAuthorize("@permissionChecker.hasPermission(authentication, 'audit_logs', 'read')")
    public ResponseEntity<List<AuditLogDTO>> getResourceHistory(
            @RequestParam String resourceType,
            @RequestParam Long resourceId) {
        Long orgId = TenantContext.getRequiredOrgId();
        log.info("리소스 변경 이력 조회: orgId={}, resourceType={}, resourceId={}",
                 orgId, resourceType, resourceId);

        List<AuditLogDTO> logs = auditLogService.getResourceHistory(orgId, resourceType, resourceId);
        return ResponseEntity.ok(logs);
    }
}
