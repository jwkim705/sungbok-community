package com.sungbok.community.controller;

import com.sungbok.community.dto.AppConfigItemDTO;
import com.sungbok.community.dto.AppConfigsResponseDTO;
import com.sungbok.community.dto.AppVersionResponseDTO;
import com.sungbok.community.dto.UpdateAppConfigRequest;
import com.sungbok.community.dto.UpdateAppVersionRequest;
import com.sungbok.community.security.TenantContext;
import com.sungbok.community.service.change.ChangeAppConfigService;
import com.sungbok.community.service.get.GetAppConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.generated.enums.PlatformType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 앱 설정 컨트롤러
 * 앱 버전 체크, 동적 설정 조회/변경 (관리자)
 */
@Slf4j
@RestController
@RequestMapping("/app-config")
@RequiredArgsConstructor
public class AppConfigController {

    private final GetAppConfigService getAppConfigService;
    private final ChangeAppConfigService changeAppConfigService;

    /**
     * 앱 버전 체크
     * Guest 모드 허용 (X-Org-Id 헤더 필수)
     *
     * @param platform 플랫폼 (ios, android)
     * @param currentVersion 현재 앱 버전
     * @return 버전 체크 결과 (needsUpdate, forceUpdate, isMaintenance 등)
     */
    @GetMapping("/version")
    public ResponseEntity<AppVersionResponseDTO> checkVersion(
            @RequestParam PlatformType platform,
            @RequestParam String currentVersion) {
        Long orgId = TenantContext.getRequiredOrgId();
        log.info("앱 버전 체크: orgId={}, platform={}, currentVersion={}", orgId, platform, currentVersion);

        AppVersionResponseDTO response = getAppConfigService.checkVersion(orgId, platform, currentVersion);
        return ResponseEntity.ok(response);
    }

    /**
     * 동적 설정 조회 (여러 키)
     * Guest 모드 허용
     *
     * @param keys 조회할 설정 키 리스트
     * @return 설정 DTO 리스트
     */
    @GetMapping
    public ResponseEntity<AppConfigsResponseDTO> getConfigs(@RequestParam List<String> keys) {
        Long orgId = TenantContext.getRequiredOrgId();
        AppConfigsResponseDTO configs = getAppConfigService.getConfigs(orgId, keys);
        return ResponseEntity.ok(configs);
    }

    /**
     * 동적 설정 조회 (단일 키)
     * Guest 모드 허용
     *
     * @param key 설정 키
     * @return 설정 DTO
     */
    @GetMapping("/{key}")
    public ResponseEntity<AppConfigItemDTO> getConfig(@PathVariable String key) {
        Long orgId = TenantContext.getRequiredOrgId();
        AppConfigItemDTO config = getAppConfigService.getConfig(orgId, key);
        return ResponseEntity.ok(config);
    }

    /**
     * 모든 동적 설정 조회
     * Guest 모드 허용
     *
     * @return 모든 설정 DTO 리스트
     */
    @GetMapping("/all")
    public ResponseEntity<AppConfigsResponseDTO> getAllConfigs() {
        Long orgId = TenantContext.getRequiredOrgId();
        AppConfigsResponseDTO configs = getAppConfigService.getAllConfigs(orgId);
        return ResponseEntity.ok(configs);
    }

    /**
     * 앱 버전 업데이트 (관리자 전용)
     *
     * @param request 업데이트할 버전 정보
     * @return 204 No Content
     */
    @PutMapping("/version")
    @PreAuthorize("@permissionChecker.hasPermission(authentication, 'app_config', 'update')")
    public ResponseEntity<Void> updateVersion(@RequestBody @Valid UpdateAppVersionRequest request) {
        Long orgId = TenantContext.getRequiredOrgId();
        log.info("앱 버전 업데이트: orgId={}, request={}", orgId, request);

        changeAppConfigService.updateVersion(orgId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * 점검 모드 설정 (관리자 전용)
     *
     * @param platform 플랫폼
     * @param isMaintenance 점검 모드 여부
     * @param message 점검 메시지
     * @return 204 No Content
     */
    @PutMapping("/maintenance")
    @PreAuthorize("@permissionChecker.hasPermission(authentication, 'app_config', 'update')")
    public ResponseEntity<Void> updateMaintenanceMode(
            @RequestParam PlatformType platform,
            @RequestParam boolean isMaintenance,
            @RequestParam(required = false) String message) {
        Long orgId = TenantContext.getRequiredOrgId();
        log.info("점검 모드 설정: orgId={}, platform={}, isMaintenance={}",
                 orgId, platform, isMaintenance);

        changeAppConfigService.updateMaintenanceMode(orgId, platform, isMaintenance, message);
        return ResponseEntity.noContent().build();
    }

    /**
     * 동적 설정 변경 (관리자 전용)
     *
     * @param request 설정 정보
     * @return 204 No Content
     */
    @PutMapping
    @PreAuthorize("@permissionChecker.hasPermission(authentication, 'app_config', 'update')")
    public ResponseEntity<Void> upsertConfig(@RequestBody @Valid UpdateAppConfigRequest request) {
        Long orgId = TenantContext.getRequiredOrgId();
        log.info("동적 설정 변경: orgId={}, configKey={}", orgId, request.getConfigKey());

        changeAppConfigService.upsertConfig(orgId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * 동적 설정 삭제 (관리자 전용)
     *
     * @param key 설정 키
     * @return 204 No Content
     */
    @DeleteMapping("/{key}")
    @PreAuthorize("@permissionChecker.hasPermission(authentication, 'app_config', 'delete')")
    public ResponseEntity<Void> deleteConfig(@PathVariable String key) {
        Long orgId = TenantContext.getRequiredOrgId();
        log.info("동적 설정 삭제: orgId={}, configKey={}", orgId, key);

        changeAppConfigService.deleteConfig(orgId, key);
        return ResponseEntity.noContent().build();
    }
}
