package com.sungbok.community.service.change.impl;

import com.sungbok.community.dto.UpdateAppConfigRequest;
import com.sungbok.community.dto.UpdateAppVersionRequest;
import com.sungbok.community.repository.AppConfigsRepository;
import com.sungbok.community.repository.AppVersionsRepository;
import com.sungbok.community.service.change.ChangeAppConfigService;
import com.sungbok.community.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.generated.enums.PlatformType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * 앱 설정 변경 서비스 구현체
 * 앱 버전 업데이트, 동적 설정 변경 (관리자 전용)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChangeAppConfigServiceImpl implements ChangeAppConfigService {

    private final AppVersionsRepository appVersionsRepository;
    private final AppConfigsRepository appConfigsRepository;
    private final RedisTemplate<String, Object> valkeyTemplate;

    @Override
    public void updateVersion(Long orgId, UpdateAppVersionRequest request) {
        Long modifiedBy = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityUtils.getUserFromAuthentication(
                        SecurityContextHolder.getContext().getAuthentication()).getUserId()
                : null;

        int updatedRows = appVersionsRepository.updateVersion(
                request.getPlatform(),
                request.getMinVersion(),
                request.getLatestVersion(),
                request.getForceUpdateMessage(),
                request.getUpdateUrl(),
                modifiedBy
        );

        if (updatedRows == 0) {
            log.warn("No app version found to update for orgId={}, platform={}", orgId, request.getPlatform());
        }

        log.info("Updated app version for orgId={}, platform={}", orgId, request.getPlatform());
    }

    @Override
    public void updateMaintenanceMode(Long orgId, PlatformType platform, boolean isMaintenance, String message) {
        Long modifiedBy = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityUtils.getUserFromAuthentication(
                        SecurityContextHolder.getContext().getAuthentication()).getUserId()
                : null;

        int updatedRows = appVersionsRepository.updateMaintenanceMode(platform, isMaintenance, message, modifiedBy);

        if (updatedRows == 0) {
            log.warn("No app version found to update maintenance mode for orgId={}, platform={}", orgId, platform);
        }

        log.info("Updated maintenance mode for orgId={}, platform={}, isMaintenance={}", orgId, platform, isMaintenance);
    }

    @Override
    public void upsertConfig(Long orgId, UpdateAppConfigRequest request) {
        appConfigsRepository.upsert(
                request.getConfigKey(),
                request.getConfigValue(),
                request.getConfigType(),
                request.getDescription()
        );

        // Valkey 캐시 무효화 (단일 키 + 목록 패턴)
        String singleKey = "app:config:" + orgId + ":" + request.getConfigKey();
        valkeyTemplate.delete(singleKey);
        // 여러 키 조회 시 사용된 캐시도 무효화 (패턴 매칭)
        Set<String> listKeys = valkeyTemplate.keys("app:config*:" + orgId + ":*");
        if (listKeys != null && !listKeys.isEmpty()) {
            valkeyTemplate.delete(listKeys);
        }
        log.info("Upserted app config for orgId={}, configKey={}, cache invalidated", orgId, request.getConfigKey());
    }

    @Override
    public void deleteConfig(Long orgId, String configKey) {
        int deletedRows = appConfigsRepository.delete(configKey);

        if (deletedRows == 0) {
            log.warn("No app config found to delete for orgId={}, configKey={}", orgId, configKey);
        }

        // Valkey 캐시 무효화
        String singleKey = "app:config:" + orgId + ":" + configKey;
        valkeyTemplate.delete(singleKey);
        Set<String> listKeys = valkeyTemplate.keys("app:config*:" + orgId + ":*");
        if (listKeys != null && !listKeys.isEmpty()) {
            valkeyTemplate.delete(listKeys);
        }
        log.info("Deleted app config for orgId={}, configKey={}, cache invalidated", orgId, configKey);
    }
}
