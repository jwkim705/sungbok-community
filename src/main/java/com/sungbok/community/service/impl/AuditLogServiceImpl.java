package com.sungbok.community.service.impl;

import com.sungbok.community.dto.AuditLogDTO;
import com.sungbok.community.repository.AuditLogsRepository;
import com.sungbok.community.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.JSONB;
import org.jooq.generated.tables.pojos.AuditLogs;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 감사 로그 서비스 구현체
 * 비동기 로깅으로 비즈니스 로직 성능 보장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogsRepository auditLogsRepository;

    @Override
    @Async
    public void log(Long orgId, Long userId, String action, String resourceType,
                    Long resourceId, JSONB oldValue, JSONB newValue,
                    String ipAddress, String userAgent) {
        try {
            AuditLogs auditLog = new AuditLogs();
            auditLog.setOrgId(orgId);
            auditLog.setUserId(userId);
            auditLog.setAction(action);
            auditLog.setResourceType(resourceType);
            auditLog.setResourceId(resourceId);
            auditLog.setOldValue(oldValue);
            auditLog.setNewValue(newValue);
            auditLog.setIpAddress(ipAddress != null ? org.jooq.types.UByte.valueOf(ipAddress) : null);
            auditLog.setUserAgent(userAgent);
            auditLog.setCreatedAt(LocalDateTime.now());

            auditLogsRepository.insert(auditLog);
            log.debug("감사 로그 기록 완료: action={}, resourceType={}, resourceId={}",
                      action, resourceType, resourceId);
        } catch (Exception e) {
            // 감사 로그 실패는 비즈니스 로직에 영향 없음
            log.error("감사 로그 기록 실패: action={}, error={}", action, e.getMessage());
        }
    }

    @Override
    public List<AuditLogDTO> getAuditLogs(Long orgId, int page, int size) {
        int limit = size;
        int offset = page * size;
        List<AuditLogs> logs = auditLogsRepository.fetchByOrgId(limit, offset);
        return logs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditLogDTO> getResourceHistory(Long orgId, String resourceType, Long resourceId) {
        List<AuditLogs> logs = auditLogsRepository.fetchByResource(resourceType, resourceId);
        return logs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * AuditLogs 엔티티를 AuditLogDTO로 변환
     */
    private AuditLogDTO convertToDTO(AuditLogs log) {
        return AuditLogDTO.builder()
                .id(log.getId())
                .orgId(log.getOrgId())
                .userId(log.getUserId())
                .action(log.getAction())
                .resourceType(log.getResourceType())
                .resourceId(log.getResourceId())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .ipAddress(log.getIpAddress() != null ? log.getIpAddress().toString() : null)
                .userAgent(log.getUserAgent())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
