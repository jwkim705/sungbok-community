package com.sungbok.community.controller;

import com.sungbok.community.repository.AppTypesRepository;
import com.sungbok.community.repository.OrganizationsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.generated.tables.pojos.AppTypes;
import org.jooq.generated.tables.pojos.Organizations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AppTypes API 컨트롤러
 * Guest mode: 앱 타입 목록 및 앱 타입별 조직 필터링
 *
 * @since 0.0.1
 */
@RestController
@RequestMapping("/app-types")
@RequiredArgsConstructor
@Slf4j
public class AppTypesController {

    private final AppTypesRepository appTypesRepository;
    private final OrganizationsRepository organizationsRepository;

    /**
     * GET /app-types
     * 모든 활성 앱 타입 조회 (Guest mode)
     *
     * @return 앱 타입 리스트
     */
    @GetMapping
    public ResponseEntity<List<AppTypes>> getAllAppTypes() {
        List<AppTypes> appTypes = appTypesRepository.fetchAllActive();
        return ResponseEntity.ok(appTypes);
    }

    /**
     * GET /app-types/{appTypeId}/organizations
     * 특정 앱 타입의 공개 조직 목록 조회 (Guest mode)
     *
     * @param appTypeId 앱 타입 ID
     * @return 공개 조직 리스트
     */
    @GetMapping("/{appTypeId}/organizations")
    public ResponseEntity<List<Organizations>> getOrganizationsByAppType(
            @PathVariable Long appTypeId) {
        List<Organizations> orgs = organizationsRepository.fetchByAppType(appTypeId);
        return ResponseEntity.ok(orgs);
    }
}
