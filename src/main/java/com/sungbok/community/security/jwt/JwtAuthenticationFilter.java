package com.sungbok.community.security.jwt;

import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.repository.UserRepository;
import com.sungbok.community.security.TenantContext;
import com.sungbok.community.security.model.PrincipalDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 인증 필터
 * Authorization 헤더에서 JWT 토큰을 추출하고 검증하여 SecurityContext에 인증 정보 설정
 * SessionHeaderFilter를 대체
 *
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final JwtProperties jwtProperties;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. Authorization 헤더에서 JWT 토큰 추출
            String jwt = extractJwtFromRequest(request);

            // 2. 토큰이 존재하고 유효한 경우 인증 처리
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                // 3. 토큰에서 이메일과 appId 추출
                String email = jwtTokenProvider.getEmailFromToken(jwt);
                Long appId = jwtTokenProvider.getAppIdFromToken(jwt);

                // 4. TenantContext에 appId 설정 (ThreadLocal)
                TenantContext.setAppId(appId);

                // 5. 데이터베이스에서 사용자 정보 조회 (appId로 필터링됨)
                UserMemberDTO user = userRepository.fetchUserWithDetailsByEmail(email)
                        .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

                // 6. appId 검증 (JWT의 appId와 DB의 appId가 일치하는지)
                if (!appId.equals(user.getAppId())) {
                    log.error("Token appId mismatch: JWT={}, DB={}", appId, user.getAppId());
                    throw new IllegalStateException("Token appId mismatch");
                }

                // 7. PrincipalDetails 생성
                PrincipalDetails principalDetails = new PrincipalDetails(user);

                // 8. Authentication 객체 생성
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principalDetails,
                                null,  // credentials (JWT에서는 불필요)
                                principalDetails.getAuthorities()
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 9. SecurityContext에 인증 정보 설정
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JWT 인증 성공: {} (appId={})", email, appId);
            }
        } catch (Exception e) {
            // 인증 실패 시 로그만 남기고 TenantContext 정리
            log.error("JWT 인증 실패: {}", e.getMessage());
            TenantContext.clear();  // 실패 시 ThreadLocal 정리
            // SecurityContext에 인증 정보를 설정하지 않으면 Spring Security가 401 반환
        }

        try {
            // 10. 다음 필터로 진행
            filterChain.doFilter(request, response);
        } finally {
            // 11. 요청 완료 후 ThreadLocal 정리 (메모리 누수 방지)
            TenantContext.clear();
        }
    }

    /**
     * Authorization 헤더에서 JWT 토큰 추출
     * 형식: "Bearer {token}"
     *
     * @param request HTTP 요청
     * @return JWT 토큰 (없으면 null)
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(jwtProperties.getHeaderName());

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(jwtProperties.getTokenPrefix())) {
            // "Bearer " 접두사 제거 (7글자)
            return bearerToken.substring(jwtProperties.getTokenPrefix().length());
        }

        return null;
    }
}
