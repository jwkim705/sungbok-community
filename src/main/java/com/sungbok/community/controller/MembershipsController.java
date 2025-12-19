package com.sungbok.community.controller;

import com.sungbok.community.common.exception.ValidationException;
import com.sungbok.community.common.exception.code.ValidationErrorCode;
import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.service.change.MembershipService;
import com.sungbok.community.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.generated.tables.pojos.Memberships;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 멤버십 API 컨트롤러
 * 가입 요청, 승인/거절, 다중 역할 관리 (겸직 지원)
 *
 * @since 0.0.1
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class MembershipsController {

    private final MembershipService membershipService;

    /**
     * POST /organizations/{orgId}/join
     * 조직 가입 요청 (Guest JWT 필요)
     *
     * @param orgId 가입할 조직 ID
     * @param authentication 현재 인증된 사용자 (Guest JWT)
     * @return 생성된 멤버십
     */
    @PostMapping("/organizations/{orgId}/join")
    public ResponseEntity<Memberships> joinOrganization(
            @PathVariable Long orgId,
            Authentication authentication) {

        UserMemberDTO user = SecurityUtils.getUserFromAuthentication(authentication);

        // Guest JWT 검증 (orgId가 null이어야 함)
        if (user.getOrgId() != null) {
            throw new ValidationException(ValidationErrorCode.FAILED,
                Map.of("error", "이미 조직에 속한 사용자입니다."));
        }

        Memberships membership = membershipService.requestJoin(
            user.getUserId(),
            orgId,
            user.getName()
        );

        return ResponseEntity.ok(membership);
    }

    /**
     * PUT /memberships/{membershipId}/approve
     * 멤버십 승인 (마을장만 가능)
     *
     * @param membershipId 멤버십 ID
     * @param authentication 현재 인증된 사용자
     * @return 성공 메시지
     */
    @PutMapping("/memberships/{membershipId}/approve")
    @PreAuthorize("@permissionChecker.hasPermission(authentication, 'users', 'update')")
    public ResponseEntity<Void> approveMembership(
            @PathVariable Long membershipId,
            Authentication authentication) {

        UserMemberDTO user = SecurityUtils.getUserFromAuthentication(authentication);
        membershipService.approveMembership(membershipId, user.getUserId());

        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /memberships/{membershipId}/reject
     * 멤버십 거절 (마을장만 가능)
     *
     * @param membershipId 멤버십 ID
     * @param authentication 현재 인증된 사용자
     * @return 성공 메시지
     */
    @PutMapping("/memberships/{membershipId}/reject")
    @PreAuthorize("@permissionChecker.hasPermission(authentication, 'users', 'update')")
    public ResponseEntity<Void> rejectMembership(
            @PathVariable Long membershipId,
            Authentication authentication) {

        UserMemberDTO user = SecurityUtils.getUserFromAuthentication(authentication);
        membershipService.rejectMembership(membershipId, user.getUserId());

        return ResponseEntity.noContent().build();
    }

    /**
     * POST /memberships/{membershipId}/roles
     * 역할 추가 (겸직 지원, 마을장만 가능)
     *
     * @param membershipId 멤버십 ID
     * @param request roleId, isPrimary (optional)
     * @param authentication 현재 인증된 사용자
     * @return 성공 메시지
     */
    @PostMapping("/memberships/{membershipId}/roles")
    @PreAuthorize("@permissionChecker.hasPermission(authentication, 'roles', 'update')")
    public ResponseEntity<Void> addMembershipRole(
            @PathVariable Long membershipId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        UserMemberDTO user = SecurityUtils.getUserFromAuthentication(authentication);

        Long roleId = request.get("roleId") != null
            ? ((Number) request.get("roleId")).longValue()
            : null;

        if (roleId == null) {
            throw new ValidationException(ValidationErrorCode.FAILED,
                Map.of("roleId", "roleId는 필수입니다."));
        }

        boolean isPrimary = request.get("isPrimary") != null
            ? (Boolean) request.get("isPrimary")
            : false;

        membershipService.addMembershipRole(membershipId, roleId, isPrimary, user.getUserId());

        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /memberships/{membershipId}/roles/{roleId}
     * 역할 제거 (마을장만 가능)
     *
     * @param membershipId 멤버십 ID
     * @param roleId 제거할 역할 ID
     * @param authentication 현재 인증된 사용자
     * @return 성공 메시지
     */
    @DeleteMapping("/memberships/{membershipId}/roles/{roleId}")
    @PreAuthorize("@permissionChecker.hasPermission(authentication, 'roles', 'update')")
    public ResponseEntity<Void> removeMembershipRole(
            @PathVariable Long membershipId,
            @PathVariable Long roleId,
            Authentication authentication) {

        UserMemberDTO user = SecurityUtils.getUserFromAuthentication(authentication);
        membershipService.removeMembershipRole(membershipId, roleId, user.getUserId());

        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /memberships/{membershipId}/roles/primary
     * 주 역할 변경 (마을장만 가능)
     *
     * @param membershipId 멤버십 ID
     * @param request roleId
     * @param authentication 현재 인증된 사용자
     * @return 성공 메시지
     */
    @PutMapping("/memberships/{membershipId}/roles/primary")
    @PreAuthorize("@permissionChecker.hasPermission(authentication, 'roles', 'update')")
    public ResponseEntity<Void> setPrimaryRole(
            @PathVariable Long membershipId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        Long roleId = request.get("roleId") != null
            ? ((Number) request.get("roleId")).longValue()
            : null;

        if (roleId == null) {
            throw new ValidationException(ValidationErrorCode.FAILED,
                Map.of("roleId", "roleId는 필수입니다."));
        }

        membershipService.setPrimaryRole(membershipId, roleId);

        return ResponseEntity.noContent().build();
    }

    /**
     * GET /memberships/pending
     * PENDING 멤버십 목록 조회 (마을장만 가능)
     *
     * @return PENDING 멤버십 리스트
     */
    @GetMapping("/memberships/pending")
    @PreAuthorize("@permissionChecker.hasPermission(authentication, 'users', 'read')")
    public ResponseEntity<List<Memberships>> getPendingMemberships() {
        List<Memberships> memberships = membershipService.getPendingMemberships();

        return ResponseEntity.ok(memberships);
    }

    /**
     * GET /memberships/me
     * 현재 사용자의 멤버십 목록 조회 (모든 조직)
     *
     * @param authentication 현재 인증된 사용자
     * @return 멤버십 리스트
     */
    @GetMapping("/memberships/me")
    public ResponseEntity<List<Memberships>> getMyMemberships(
            Authentication authentication) {

        UserMemberDTO user = SecurityUtils.getUserFromAuthentication(authentication);
        List<Memberships> memberships = membershipService.getMyMemberships(user.getUserId());

        return ResponseEntity.ok(memberships);
    }
}
