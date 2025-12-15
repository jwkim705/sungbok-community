package com.sungbok.community.controller;

import com.sungbok.community.common.dto.OkResponseDTO;
import com.sungbok.community.common.exception.DataNotFoundException;
import com.sungbok.community.repository.OrganizationsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.generated.tables.pojos.Organizations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<OkResponseDTO> getAllPublicOrganizations() {
        List<Organizations> orgs = organizationsRepository.fetchAllPublic();
        return ResponseEntity.ok(
            OkResponseDTO.of(200, "공개 조직 목록 조회 성공", orgs)
        );
    }

    /**
     * GET /organizations/{orgId}
     * 특정 조직 상세 조회 (Guest mode)
     *
     * @param orgId 조직 ID
     * @return 조직 상세 정보
     */
    @GetMapping("/{orgId}")
    public ResponseEntity<OkResponseDTO> getOrganizationById(
            @PathVariable Long orgId) {
        Organizations org = organizationsRepository.fetchById(orgId)
                .filter(Organizations::getIsPublic)
                .orElseThrow(() -> new DataNotFoundException(
                    HttpStatus.NOT_FOUND,
                    "조직을 찾을 수 없거나 공개되지 않은 조직입니다: " + orgId
                ));
        return ResponseEntity.ok(
            OkResponseDTO.of(200, "조직 조회 성공", org)
        );
    }
}
