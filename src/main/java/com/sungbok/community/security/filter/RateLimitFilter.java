package com.sungbok.community.security.filter;

import com.sungbok.community.common.exception.AuthenticationException;
import com.sungbok.community.common.exception.code.AuthErrorCode;
import com.sungbok.community.security.model.PrincipalDetails;
import com.sungbok.community.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rate Limiting 필터
 * JwtAuthenticationFilter 이후 실행
 * Valkey 기반 요청 제한 (100req/min)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 사용자 식별자 추출
        String identifier = extractIdentifier(request);
        String endpoint = request.getRequestURI();

        // Rate limit 체크
        boolean allowed = rateLimitService.isAllowed(identifier, endpoint);
        if (!allowed) {
            throw new AuthenticationException(AuthErrorCode.ACCESS_DENIED);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 사용자 식별자 추출
     * 인증된 사용자: user:{userId}
     * Guest: ip:{ipAddress}
     */
    private String extractIdentifier(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
            && !"anonymousUser".equals(authentication.getPrincipal())) {
            // 인증된 사용자
            Object principal = authentication.getPrincipal();
            if (principal instanceof PrincipalDetails) {
                Long userId = ((PrincipalDetails) principal).getUser().getUserId();
                return "user:" + userId;
            }
        }
        // Guest: IP 주소 사용
        String ip = extractIpAddress(request);
        return "ip:" + ip;
    }

    /**
     * IP 주소 추출 (X-Forwarded-For 우선)
     */
    private String extractIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
