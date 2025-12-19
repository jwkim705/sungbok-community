package com.sungbok.community.controller;

import com.sungbok.community.dto.AgreeToTermsRequest;
import com.sungbok.community.dto.CreateTermRequest;
import com.sungbok.community.dto.TermDTO;
import com.sungbok.community.dto.UserTermAgreementDTO;
import com.sungbok.community.security.TenantContext;
import com.sungbok.community.service.change.ChangeTermsService;
import com.sungbok.community.service.get.GetTermsService;
import com.sungbok.community.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.generated.enums.TermType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 약관 컨트롤러
 * 현재 약관 조회, 약관 동의 처리, 약관 관리 (관리자)
 */
@Slf4j
@RestController
@RequestMapping("/terms")
@RequiredArgsConstructor
public class TermsController {

    private final GetTermsService getTermsService;
    private final ChangeTermsService changeTermsService;

    /**
     * 현재 유효한 약관 조회
     * Guest 모드 허용 (X-Org-Id 헤더 선택적)
     *
     * @return 현재 약관 리스트 (TOS, PRIVACY, MARKETING)
     */
    @GetMapping("/current")
    public ResponseEntity<List<TermDTO>> getCurrentTerms() {
        Long orgId = TenantContext.getOrgId(); // Nullable (플랫폼 전체 약관)
        log.info("현재 약관 조회: orgId={}", orgId);

        List<TermDTO> terms = getTermsService.getCurrentTerms(orgId);
        return ResponseEntity.ok(terms);
    }

    /**
     * 약관 동의 처리
     * 인증 필수, IP 주소 자동 추출
     *
     * @param request 동의할 약관 ID 리스트
     * @param authentication 현재 인증 정보
     * @param httpRequest HTTP 요청 (IP 추출용)
     * @return 204 No Content
     */
    @PostMapping("/agree")
    public ResponseEntity<Void> agreeToTerms(
            @RequestBody @Valid AgreeToTermsRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        Long userId = SecurityUtils.getUserFromAuthentication(authentication).getUserId();
        String ipAddress = extractIpAddress(httpRequest);

        log.info("사용자 약관 동의: userId={}, termIds={}, ip={}", userId, request.getTermIds(), ipAddress);
        changeTermsService.agreeToTerms(userId, request, ipAddress);
        return ResponseEntity.noContent().build();
    }

    /**
     * 사용자 약관 동의 이력 조회
     * 본인 또는 관리자만 조회 가능
     *
     * @param userId 사용자 ID
     * @param authentication 현재 인증 정보
     * @return 동의 이력 리스트
     */
    @GetMapping("/{userId}/agreements")
    @PreAuthorize("@permissionChecker.isOwnerOrAdmin(authentication, #userId)")
    public ResponseEntity<List<UserTermAgreementDTO>> getUserAgreements(
            @PathVariable Long userId,
            Authentication authentication) {
        log.info("사용자 약관 동의 이력 조회: userId={}", userId);

        List<UserTermAgreementDTO> agreements = getTermsService.getUserAgreements(userId);
        return ResponseEntity.ok(agreements);
    }

    /**
     * 약관 버전 이력 조회 (관리자 전용)
     *
     * @param termType 약관 타입
     * @return 약관 버전 리스트
     */
    @GetMapping("/history")
    @PreAuthorize("@permissionChecker.hasPermission(authentication, 'terms', 'read')")
    public ResponseEntity<List<TermDTO>> getTermVersionHistory(@RequestParam TermType termType) {
        Long orgId = TenantContext.getOrgId();
        log.info("약관 버전 이력 조회: orgId={}, termType={}", orgId, termType);

        List<TermDTO> terms = getTermsService.getTermVersionHistory(orgId, termType);
        return ResponseEntity.ok(terms);
    }

    /**
     * 약관 생성 (관리자 전용)
     *
     * @param request 약관 정보
     * @return 생성된 약관
     */
    @PostMapping
    @PreAuthorize("@permissionChecker.hasPermission(authentication, 'terms', 'create')")
    public ResponseEntity<TermDTO> createTerm(@RequestBody @Valid CreateTermRequest request) {
        Long orgId = TenantContext.getOrgId(); // Nullable (플랫폼 약관)
        log.info("약관 생성: orgId={}, termType={}, version={}", orgId, request.getTermType(), request.getVersion());

        TermDTO term = changeTermsService.createTerm(orgId, request);
        return ResponseEntity.ok(term);
    }

    /**
     * 현재 약관 설정 (관리자 전용)
     *
     * @param termType 약관 타입
     * @param termId 약관 ID
     * @return 204 No Content
     */
    @PutMapping("/current")
    @PreAuthorize("@permissionChecker.hasPermission(authentication, 'terms', 'update')")
    public ResponseEntity<Void> setCurrentTerm(
            @RequestParam TermType termType,
            @RequestParam Long termId) {
        Long orgId = TenantContext.getOrgId();
        log.info("현재 약관 설정: orgId={}, termType={}, termId={}", orgId, termType, termId);

        changeTermsService.setCurrentTerm(orgId, termType, termId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 약관 삭제 (관리자 전용)
     * 현재(current) 약관은 삭제 불가
     *
     * @param termId 약관 ID
     * @return 204 No Content
     */
    @DeleteMapping("/{termId}")
    @PreAuthorize("@permissionChecker.hasPermission(authentication, 'terms', 'delete')")
    public ResponseEntity<Void> deleteTerm(@PathVariable Long termId) {
        Long orgId = TenantContext.getOrgId();
        log.info("약관 삭제: orgId={}, termId={}", orgId, termId);

        changeTermsService.deleteTerm(orgId, termId);
        return ResponseEntity.noContent().build();
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
        // X-Forwarded-For: client, proxy1, proxy2 형태일 경우 첫 번째 IP 추출
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
