package com.sungbok.community.aspect;

import com.sungbok.community.annotation.Auditable;
import com.sungbok.community.security.TenantContext;
import com.sungbok.community.service.AuditLogService;
import com.sungbok.community.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @Auditable AOP Aspect
 * 메서드 실행 시 자동으로 감사 로그 기록
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditableAspect {

    private final AuditLogService auditLogService;

    @Around("@annotation(auditable)")
    public Object logAudit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        // 메서드 실행
        Object result = joinPoint.proceed();

        try {
            // 감사 로그 정보 추출
            Long orgId = TenantContext.getOrgId();
            Long userId = extractUserId();
            String action = auditable.action();
            String resourceType = auditable.resourceType().isEmpty() ? null : auditable.resourceType();
            Long resourceId = extractResourceId(joinPoint);

            HttpServletRequest request = getCurrentRequest();
            String ipAddress = request != null ? extractIpAddress(request) : null;
            String userAgent = request != null ? request.getHeader("User-Agent") : null;

            // 비동기 로그 기록
            auditLogService.log(orgId, userId, action, resourceType, resourceId,
                               null, null, ipAddress, userAgent);

        } catch (Exception e) {
            // 감사 로그 실패는 비즈니스 로직에 영향 없음
            log.error("감사 로그 기록 실패: action={}, error={}", auditable.action(), e.getMessage());
        }

        return result;
    }

    /**
     * 현재 인증된 사용자 ID 추출
     */
    private Long extractUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return SecurityUtils.getUserFromAuthentication(authentication).getUserId();
            }
        } catch (Exception e) {
            log.debug("사용자 ID 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 메서드 파라미터에서 resourceId 추출 (첫 번째 Long 타입)
     */
    private Long extractResourceId(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
        }
        return null;
    }

    /**
     * 현재 HTTP 요청 가져오기
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * IP 주소 추출
     */
    private String extractIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
