package com.sungbok.community.controller;

import com.sungbok.community.common.dto.OkResponseDTO;
import com.sungbok.community.repository.RolesRepository;
import com.sungbok.community.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.jooq.generated.tables.pojos.Roles;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 역할 조회 API (앱용)
 * 프론트엔드에서 roleIds 배열로 역할 정보 표시용
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/roles")
public class RoleController {

    private final RolesRepository rolesRepository;

    /**
     * 현재 조직의 모든 역할 조회
     *
     * @return 역할 목록 (level, name 순 정렬)
     */
    @GetMapping
    public ResponseEntity<OkResponseDTO> getRoles() {
        Long orgId = TenantContext.getRequiredOrgId();
        List<Roles> roles = rolesRepository.fetchAllByOrgId(orgId);
        return ResponseEntity.ok(
            OkResponseDTO.of(200, "Roles retrieved", roles)
        );
    }
}
