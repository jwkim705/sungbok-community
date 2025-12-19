package com.sungbok.community.security.jwt;

import com.sungbok.community.common.exception.ValidationException;
import com.sungbok.community.common.exception.code.AuthErrorCode;
import com.sungbok.community.common.exception.code.TenantErrorCode;
import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.repository.UserRepository;
import com.sungbok.community.security.TenantContext;
import com.sungbok.community.security.model.PrincipalDetails;
import com.sungbok.community.security.util.FilterErrorResponseUtil;
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
import java.util.List;

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
    private final TenantResolver tenantResolver;

    /**
     * Form Login 경로는 필터링하지 않음
     * UsernamePasswordAuthenticationFilter가 처리하도록 제외
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        // Form Login 엔드포인트 제외
        return path.equals("/login");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. Authorization 헤더에서 JWT 토큰 추출
            String jwt = extractJwtFromRequest(request);

            // 2. 토큰이 존재하고 유효한 경우 인증 처리
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                // 3. 토큰에서 이메일과 orgId 추출
                String email = jwtTokenProvider.getEmailFromToken(jwt);
                Long orgId = jwtTokenProvider.getOrgIdFromToken(jwt);

                // ⚠️ Guest JWT 처리 (orgId=null)
                if (orgId == null) {
                    // Guest JWT: 인증은 되었지만 조직에 속하지 않음
                    // 플랫폼 레벨 API만 허용
                    String requestURI = request.getRequestURI();
                    boolean isPlatformApi = requestURI.endsWith("/organizations")
                                         || requestURI.endsWith("/app-types")
                                         || requestURI.matches(".*/organizations/[0-9]+/join")
                                         || requestURI.endsWith("/memberships/me");

                    if (!isPlatformApi) {
                        log.error("Guest JWT는 플랫폼 레벨 API만 접근 가능: {}", requestURI);
                        FilterErrorResponseUtil.writeErrorResponse(
                                response,
                                AuthErrorCode.ACCESS_DENIED,
                                requestURI
                        );
                        return;  // 필터 체인 중단
                    }

                    // Guest 인증 정보 설정 (orgId 없이)
                    UserMemberDTO guestUser = new UserMemberDTO(
                        null,  // orgId (Guest 사용자)
                        jwtTokenProvider.getUserIdFromToken(jwt),
                        email,
                        jwtTokenProvider.getNameFromToken(jwt),
                        null, null, null, null, null, null, null, null,
                        List.of()  // roleIds (빈 리스트)
                    );

                    PrincipalDetails principalDetails = new PrincipalDetails(guestUser);
                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                            principalDetails, null, principalDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Guest JWT 인증 성공: {}", email);

                } else {
                    // AUTHENTICATED: JWT's orgId is authoritative

                    // 4. TenantContext에 orgId 설정 (ThreadLocal)
                    TenantContext.setOrgId(orgId);

                    // 5. 데이터베이스에서 사용자 정보 조회 (orgId로 필터링됨)
                    UserMemberDTO user = userRepository.fetchUserWithDetailsByEmail(email)
                            .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

                    // 6. orgId 검증 (JWT의 orgId와 DB의 orgId가 일치하는지)
                    if (!orgId.equals(user.getOrgId())) {
                        log.error("Token orgId mismatch: JWT={}, DB={}", orgId, user.getOrgId());
                        throw new IllegalStateException("Token orgId mismatch");
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
                    log.debug("JWT 인증 성공: {} (orgId={})", email, orgId);
                }
            } else {
                // GUEST MODE: X-Org-Id 헤더로 조직 해결
                // 플랫폼 레벨 API 및 OAuth 로그인은 X-Org-Id 선택적
                String requestURI = request.getRequestURI();
                // MockMvc 테스트에서는 context path 없이 "/organizations"로 들어오고,
                // 실제 배포에서는 "/api/organizations"로 들어올 수 있음
                boolean isPlatformLevelApi = requestURI.endsWith("/organizations")
                                          || requestURI.endsWith("/app-types")
                                          || requestURI.contains("/auth/");  // 모든 /auth/** 경로는 플랫폼 레벨

                if (isPlatformLevelApi) {
                    // 플랫폼 레벨 API: X-Org-Id 선택적 (있으면 사용, 없어도 허용)
                    try {
                        Long orgId = tenantResolver.resolveOrgId(request);
                        TenantContext.setOrgId(orgId);
                        log.debug("플랫폼 레벨 API 접근 (X-Org-Id 제공됨): orgId={}", requestURI);
                    } catch (ValidationException e) {
                        // X-Org-Id 없어도 허용
                        log.debug("플랫폼 레벨 API 접근 (X-Org-Id 없음): {}", requestURI);
                    }
                } else {
                    // 일반 Guest mode API: X-Org-Id 필수
                    try {
                        Long orgId = tenantResolver.resolveOrgId(request);
                        TenantContext.setOrgId(orgId);
                        log.debug("Guest 모드: orgId={}", orgId);
                    } catch (ValidationException e) {
                        log.error("Guest 모드 검증 실패: {}", e.getMessage());
                        FilterErrorResponseUtil.writeErrorResponse(
                                response,
                                TenantErrorCode.NOT_FOUND,
                                request.getRequestURI()
                        );
                        return;  // 필터 체인 중단
                    }
                }
                // No authentication - guest user (read-only)
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
