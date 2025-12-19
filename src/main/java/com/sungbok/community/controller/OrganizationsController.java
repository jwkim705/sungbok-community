package com.sungbok.community.controller;

import com.sungbok.community.common.exception.ResourceNotFoundException;
import com.sungbok.community.common.exception.code.TenantErrorCode;
import com.sungbok.community.repository.OrganizationsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.generated.tables.pojos.Organizations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Organizations API 컨트롤러
 * Guest mode: 공개 조직 목록 조회
 *
 * @since 0.0.1
 */
@RestController
@RequestMapping("/organizations")
@RequiredArgsConstructor
@Slf4j
public class OrganizationsController {

    private final OrganizationsRepository organizationsRepository;

    /**
     * GET /organizations
     * 모든 공개 조직 목록 조회 (Guest mode)
     *
     * @return 공개 조직 리스트
     */
    @GetMapping
    public ResponseEntity<List<Organizations>> getAllPublicOrganizations() {
        List<Organizations> orgs = organizationsRepository.fetchAllPublic();
        return ResponseEntity.ok(orgs);
    }

    /**
     * GET /organizations/{orgId}
     * 특정 조직 상세 조회 (Guest mode)
     *
     * @param orgId 조직 ID
     * @return 조직 상세 정보
     */
    @GetMapping("/{orgId}")
    public ResponseEntity<Organizations> getOrganizationById(
            @PathVariable Long orgId) {
        Organizations org = organizationsRepository.fetchById(orgId)
                .filter(Organizations::getIsPublic)
                .orElseThrow(() -> new ResourceNotFoundException(
                    TenantErrorCode.NOT_FOUND,
                    Map.of("orgId", orgId)
                ));
        return ResponseEntity.ok(org);
    }
}
