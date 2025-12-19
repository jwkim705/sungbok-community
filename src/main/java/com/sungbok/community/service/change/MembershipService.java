package com.sungbok.community.service.change;

import com.sungbok.community.common.exception.ResourceNotFoundException;
import com.sungbok.community.common.exception.ValidationException;
import com.sungbok.community.common.exception.code.ResourceErrorCode;
import com.sungbok.community.common.exception.code.TenantErrorCode;
import com.sungbok.community.common.exception.code.ValidationErrorCode;
import com.sungbok.community.dto.event.NotificationEvent;
import org.jooq.generated.enums.MembershipStatus;
import com.sungbok.community.enums.NotificationType;
import com.sungbok.community.repository.MembersRepository;
import com.sungbok.community.repository.MembershipRolesRepository;
import com.sungbok.community.repository.OrganizationsRepository;
import com.sungbok.community.security.TenantContext;
import com.sungbok.community.service.RedisQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.generated.tables.pojos.Memberships;
import org.jooq.generated.tables.pojos.Organizations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 멤버십 관리 서비스
 * 가입 요청, 승인/거절, 다중 역할 관리 (겸직 지원)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipService {

    private final MembersRepository membersRepository;
    private final MembershipRolesRepository membershipRolesRepository;
    private final OrganizationsRepository organizationsRepository;
    private final RedisQueueService queueService;

    /**
     * 조직 가입 요청 (Guest JWT 사용자)
     *
     * @param userId 사용자 ID (Guest JWT)
     * @param orgId 가입할 조직 ID
     * @param userName OAuth에서 가져온 사용자 이름
     * @return 생성된 멤버십 (PENDING 상태)
     */
    @Transactional
    public Memberships requestJoin(Long userId, Long orgId, String userName) {
        // 1. 조직 존재 및 공개 여부 확인
        Organizations org = organizationsRepository.fetchById(orgId)
                .filter(Organizations::getIsPublic)
                .orElseThrow(() -> new ResourceNotFoundException(
                    TenantErrorCode.NOT_FOUND,
                    Map.of("orgId", orgId)
                ));

        // 2. 중복 가입 방지 (이미 PENDING/APPROVED 멤버십이 있는지 확인)
        membersRepository.fetchByUserIdAndOrgId(userId, orgId)
                .ifPresent(m -> {
                    throw new ValidationException(
                        ValidationErrorCode.FAILED,
                        Map.of("status", m.getStatus().name(), "message", "이미 해당 조직에 가입 요청하였거나 멤버입니다")
                    );
                });

        // 3. 멤버십 생성 (PENDING)
        Memberships membership = new Memberships();
        membership.setOrgId(orgId);
        membership.setUserId(userId);
        membership.setName(userName);  // OAuth에서 가져온 이름
        membership.setStatus(MembershipStatus.PENDING);
        membership.setRequestedAt(LocalDateTime.now());
        membership.setCreatedAt(LocalDateTime.now());

        // ⚠️ TenantContext 설정 후 insert (org-scoped table)
        TenantContext.setOrgId(orgId);
        Memberships created = membersRepository.insert(membership);
        TenantContext.clear();

        log.info("가입 요청 생성: userId={}, orgId={}, membershipId={}",
            userId, orgId, created.getId());
        return created;
    }

    /**
     * 멤버십 승인 (마을장만 가능)
     *
     * @param membershipId 멤버십 ID
     * @param approverId 승인자 ID
     */
    @Transactional
    public void approveMembership(Long membershipId, Long approverId) {
        Long orgId = TenantContext.getRequiredOrgId();

        // 1. 멤버십 조회
        Memberships membership = membersRepository.fetchById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    ResourceErrorCode.NOT_FOUND,
                    Map.of("membershipId", membershipId)
                ));

        // 2. 상태 검증
        if (!MembershipStatus.PENDING.equals(membership.getStatus())) {
            throw new ValidationException(
                ValidationErrorCode.FAILED,
                Map.of("status", membership.getStatus().name(), "message", "PENDING 상태의 멤버십만 승인할 수 있습니다")
            );
        }

        // 3. 멤버십 상태 업데이트
        int updated = membersRepository.updateStatus(membershipId, MembershipStatus.APPROVED, approverId);
        if (updated == 0) {
            throw new IllegalStateException("멤버십 승인 실패: " + membershipId);
        }

        // 4. 기본 역할(성도, level=1) 자동 할당
        membershipRolesRepository.assignDefaultRole(membershipId, orgId, approverId);

        // 5. 알림 발송 (MEMBERSHIP_APPROVED)
        NotificationEvent event = NotificationEvent.builder()
                .orgId(orgId)
                .userId(membership.getUserId())
                .notificationType(NotificationType.MEMBERSHIP_APPROVED)
                .title("멤버십 승인")
                .body("귀하의 가입 요청이 승인되었습니다.")
                .relatedEntityType("membership")
                .relatedEntityId(membershipId)
                .data(Map.of("membershipId", membershipId, "orgId", orgId))
                .build();
        queueService.enqueue(event);

        log.info("멤버십 승인 완료: membershipId={}, approverId={}", membershipId, approverId);
    }

    /**
     * 멤버십 거절 (마을장만 가능)
     *
     * @param membershipId 멤버십 ID
     * @param approverId 승인자 ID (거절자)
     */
    @Transactional
    public void rejectMembership(Long membershipId, Long approverId) {
        // 1. 멤버십 조회
        Memberships membership = membersRepository.fetchById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    ResourceErrorCode.NOT_FOUND,
                    Map.of("membershipId", membershipId)
                ));

        // 2. 상태 검증
        if (!MembershipStatus.PENDING.equals(membership.getStatus())) {
            throw new ValidationException(
                ValidationErrorCode.FAILED,
                Map.of("status", membership.getStatus().name(), "message", "PENDING 상태의 멤버십만 거절할 수 있습니다")
            );
        }

        // 3. 멤버십 상태 업데이트
        int updated = membersRepository.updateStatus(membershipId, MembershipStatus.REJECTED, approverId);
        if (updated == 0) {
            throw new IllegalStateException("멤버십 거절 실패: " + membershipId);
        }

        // 4. 알림 발송 (MEMBERSHIP_REJECTED)
        Long orgId = TenantContext.getRequiredOrgId();
        NotificationEvent event = NotificationEvent.builder()
                .orgId(orgId)
                .userId(membership.getUserId())
                .notificationType(NotificationType.MEMBERSHIP_REJECTED)
                .title("멤버십 거절")
                .body("귀하의 가입 요청이 거절되었습니다.")
                .relatedEntityType("membership")
                .relatedEntityId(membershipId)
                .data(Map.of("membershipId", membershipId, "orgId", orgId))
                .build();
        queueService.enqueue(event);

        log.info("멤버십 거절 완료: membershipId={}, approverId={}", membershipId, approverId);
    }

    /**
     * 역할 추가 (겸직 지원)
     *
     * @param membershipId 멤버십 ID
     * @param roleId 추가할 역할 ID
     * @param isPrimary 주 역할 여부
     * @param assignerId 할당자 ID
     */
    @Transactional
    public void addMembershipRole(Long membershipId, Long roleId, boolean isPrimary, Long assignerId) {
        // 1. 멤버십 조회 및 검증
        Memberships membership = membersRepository.fetchById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    ResourceErrorCode.NOT_FOUND,
                    Map.of("membershipId", membershipId)
                ));

        if (!MembershipStatus.APPROVED.equals(membership.getStatus())) {
            throw new ValidationException(
                ValidationErrorCode.FAILED,
                Map.of("status", membership.getStatus().name(), "message", "APPROVED 상태의 멤버십만 역할 추가 가능합니다")
            );
        }

        // 2. 역할 추가 (중복 체크 + Primary 관리)
        membershipRolesRepository.addRole(membershipId, roleId, isPrimary, assignerId);

        log.info("역할 추가 완료: membershipId={}, roleId={}, isPrimary={}, assignerId={}",
            membershipId, roleId, isPrimary, assignerId);
    }

    /**
     * 역할 제거 (겸직 해제)
     *
     * @param membershipId 멤버십 ID
     * @param roleId 제거할 역할 ID
     * @param assignerId 할당자 ID (로깅용)
     */
    @Transactional
    public void removeMembershipRole(Long membershipId, Long roleId, Long assignerId) {
        // 1. 멤버십 조회 및 검증
        Memberships membership = membersRepository.fetchById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    ResourceErrorCode.NOT_FOUND,
                    Map.of("membershipId", membershipId)
                ));

        if (!MembershipStatus.APPROVED.equals(membership.getStatus())) {
            throw new ValidationException(
                ValidationErrorCode.FAILED,
                Map.of("status", membership.getStatus().name(), "message", "APPROVED 상태의 멤버십만 역할 제거 가능합니다")
            );
        }

        // 2. 마지막 역할 제거 방지 (최소 1개 역할 유지)
        List<Long> currentRoles = membershipRolesRepository.fetchRoleIdsByMembershipId(membershipId);
        if (currentRoles.size() <= 1) {
            throw new ValidationException(
                ValidationErrorCode.FAILED,
                Map.of("message", "마지막 역할은 제거할 수 없습니다. 최소 1개 역할 필요.")
            );
        }

        // 3. 역할 제거
        int deleted = membershipRolesRepository.removeRole(membershipId, roleId);
        if (deleted == 0) {
            throw new ResourceNotFoundException(
                ResourceErrorCode.NOT_FOUND,
                Map.of("roleId", roleId, "membershipId", membershipId)
            );
        }

        log.info("역할 제거 완료: membershipId={}, roleId={}, assignerId={}",
            membershipId, roleId, assignerId);
    }

    /**
     * 주 역할 변경
     *
     * @param membershipId 멤버십 ID
     * @param roleId 주 역할로 설정할 역할 ID
     */
    @Transactional
    public void setPrimaryRole(Long membershipId, Long roleId) {
        // 1. 멤버십 조회 및 검증
        Memberships membership = membersRepository.fetchById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    ResourceErrorCode.NOT_FOUND,
                    Map.of("membershipId", membershipId)
                ));

        if (!MembershipStatus.APPROVED.equals(membership.getStatus())) {
            throw new ValidationException(
                ValidationErrorCode.FAILED,
                Map.of("status", membership.getStatus().name(), "message", "APPROVED 상태의 멤버십만 주 역할 변경 가능합니다")
            );
        }

        // 2. 주 역할 변경
        int updated = membershipRolesRepository.setPrimaryRole(membershipId, roleId);
        if (updated == 0) {
            throw new ResourceNotFoundException(
                ResourceErrorCode.NOT_FOUND,
                Map.of("roleId", roleId, "membershipId", membershipId)
            );
        }

        log.info("주 역할 변경 완료: membershipId={}, newPrimaryRoleId={}", membershipId, roleId);
    }

    /**
     * PENDING 멤버십 목록 조회 (관리자용)
     *
     * @return PENDING 멤버십 리스트
     */
    @Transactional(readOnly = true)
    public List<Memberships> getPendingMemberships() {
        return membersRepository.fetchPendingMemberships();
    }

    /**
     * 현재 사용자의 멤버십 목록 조회 (모든 조직)
     *
     * @param userId 사용자 ID
     * @return 멤버십 리스트
     */
    @Transactional(readOnly = true)
    public List<Memberships> getMyMemberships(Long userId) {
        return membersRepository.fetchAllByUserId(userId);
    }
}
