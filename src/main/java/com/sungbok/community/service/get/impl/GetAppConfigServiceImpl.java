package com.sungbok.community.service.get.impl;

import tools.jackson.databind.ObjectMapper;
import com.sungbok.community.common.exception.ResourceNotFoundException;
import com.sungbok.community.common.exception.code.TenantErrorCode;
import com.sungbok.community.dto.AppConfigItemDTO;
import com.sungbok.community.dto.AppConfigsResponseDTO;
import com.sungbok.community.dto.AppVersionResponseDTO;
import com.sungbok.community.repository.AppConfigsRepository;
import com.sungbok.community.repository.AppVersionsRepository;
import com.sungbok.community.service.get.GetAppConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.generated.enums.PlatformType;
import org.jooq.generated.tables.pojos.AppConfigs;
import org.jooq.generated.tables.pojos.AppVersions;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 앱 설정 조회 서비스 구현체
 * 앱 버전 체크, 동적 설정 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetAppConfigServiceImpl implements GetAppConfigService {

    private final AppVersionsRepository appVersionsRepository;
    private final AppConfigsRepository appConfigsRepository;
    private final RedisTemplate<String, Object> valkeyTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public AppVersionResponseDTO checkVersion(Long orgId, PlatformType platform, String currentVersion) {
        AppVersions appVersion = appVersionsRepository.fetchByOrgAndPlatform(platform)
                .orElseThrow(() -> new ResourceNotFoundException(
                        TenantErrorCode.NOT_FOUND,
                        Map.of("orgId", orgId, "platform", platform.getLiteral())
                ));

        // 점검 모드 체크
        if (Boolean.TRUE.equals(appVersion.getIsMaintenance())) {
            return AppVersionResponseDTO.builder()
                    .needsUpdate(false)
                    .forceUpdate(false)
                    .latestVersion(appVersion.getLatestVersion())
                    .isMaintenance(true)
                    .maintenanceMessage(appVersion.getMaintenanceMessage())
                    .build();
        }

        // 버전 비교
        boolean needsUpdate = compareVersion(currentVersion, appVersion.getLatestVersion()) < 0;
        boolean forceUpdate = compareVersion(currentVersion, appVersion.getMinVersion()) < 0;

        return AppVersionResponseDTO.builder()
                .needsUpdate(needsUpdate)
                .forceUpdate(forceUpdate)
                .latestVersion(appVersion.getLatestVersion())
                .message(forceUpdate ? appVersion.getForceUpdateMessage() : "새로운 버전이 있습니다.")
                .updateUrl(appVersion.getUpdateUrl())
                .isMaintenance(false)
                .build();
    }

    @Override
    public AppConfigsResponseDTO getConfigs(Long orgId, List<String> configKeys) {
        try {
            String cacheKey = "app:configs:" + orgId + ":" + String.join(",", configKeys);
            String cachedJson = (String) valkeyTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                // JSON을 AppConfigsResponseDTO로 역직렬화
                AppConfigsResponseDTO cached = objectMapper.readValue(cachedJson, AppConfigsResponseDTO.class);
                return cached;
            }
            // Cache miss - DB 조회 후 캐싱
            List<AppConfigs> configs = appConfigsRepository.fetchByOrgIdAndKeys(configKeys);
            List<AppConfigItemDTO> items = configs.stream()
                    .map(config -> AppConfigItemDTO.builder()
                            .configKey(config.getConfigKey())
                            .configValue(config.getConfigValue())
                            .configType(config.getConfigType())
                            .description(config.getDescription())
                            .build())
                    .collect(Collectors.toList());
            AppConfigsResponseDTO result = new AppConfigsResponseDTO(items);
            // DTO를 JSON으로 직렬화하여 캐싱
            String resultJson = objectMapper.writeValueAsString(result);
            valkeyTemplate.opsForValue().set(cacheKey, resultJson, Duration.ofHours(24));
            return result;
        } catch (Exception e) {
            log.error("Valkey 캐싱 처리 실패, DB에서 직접 조회: {}", e.getMessage());
            // 예외 발생 시 DB에서 직접 조회
            List<AppConfigs> configs = appConfigsRepository.fetchByOrgIdAndKeys(configKeys);
            List<AppConfigItemDTO> items = configs.stream()
                    .map(config -> AppConfigItemDTO.builder()
                            .configKey(config.getConfigKey())
                            .configValue(config.getConfigValue())
                            .configType(config.getConfigType())
                            .description(config.getDescription())
                            .build())
                    .collect(Collectors.toList());
            return new AppConfigsResponseDTO(items);
        }
    }

    @Override
    public AppConfigItemDTO getConfig(Long orgId, String configKey) {
        try {
            String cacheKey = "app:config:" + orgId + ":" + configKey;
            String cachedJson = (String) valkeyTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                // JSON을 AppConfigItemDTO로 역직렬화
                AppConfigItemDTO cached = objectMapper.readValue(cachedJson, AppConfigItemDTO.class);
                return cached;
            }
            // Cache miss - DB 조회 후 캐싱
            Optional<AppConfigs> configOpt = appConfigsRepository.fetchByOrgIdAndKey(configKey);
            if (configOpt.isEmpty()) {
                return null;
            }
            AppConfigs config = configOpt.get();
            AppConfigItemDTO item = AppConfigItemDTO.builder()
                    .configKey(config.getConfigKey())
                    .configValue(config.getConfigValue())
                    .configType(config.getConfigType())
                    .description(config.getDescription())
                    .build();
            // DTO를 JSON으로 직렬화하여 캐싱
            String itemJson = objectMapper.writeValueAsString(item);
            valkeyTemplate.opsForValue().set(cacheKey, itemJson, Duration.ofHours(24));
            return item;
        } catch (Exception e) {
            log.error("Valkey 캐싱 처리 실패, DB에서 직접 조회: {}", e.getMessage());
            // 예외 발생 시 DB에서 직접 조회
            return appConfigsRepository.fetchByOrgIdAndKey(configKey)
                    .map(config -> AppConfigItemDTO.builder()
                            .configKey(config.getConfigKey())
                            .configValue(config.getConfigValue())
                            .configType(config.getConfigType())
                            .description(config.getDescription())
                            .build())
                    .orElse(null);
        }
    }

    @Override
    public AppConfigsResponseDTO getAllConfigs(Long orgId) {
        try {
            String cacheKey = "app:configs:all:" + orgId;
            String cachedJson = (String) valkeyTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                // JSON을 AppConfigsResponseDTO로 역직렬화
                AppConfigsResponseDTO cached = objectMapper.readValue(cachedJson, AppConfigsResponseDTO.class);
                return cached;
            }
            // Cache miss - DB 조회 후 캐싱
            List<AppConfigs> configs = appConfigsRepository.fetchAllByOrgId();
            List<AppConfigItemDTO> items = configs.stream()
                    .map(config -> AppConfigItemDTO.builder()
                            .configKey(config.getConfigKey())
                            .configValue(config.getConfigValue())
                            .configType(config.getConfigType())
                            .description(config.getDescription())
                            .build())
                    .collect(Collectors.toList());
            AppConfigsResponseDTO result = new AppConfigsResponseDTO(items);
            // DTO를 JSON으로 직렬화하여 캐싱
            String resultJson = objectMapper.writeValueAsString(result);
            valkeyTemplate.opsForValue().set(cacheKey, resultJson, Duration.ofHours(24));
            return result;
        } catch (Exception e) {
            log.error("Valkey 캐싱 처리 실패, DB에서 직접 조회: {}", e.getMessage());
            // 예외 발생 시 DB에서 직접 조회
            List<AppConfigs> configs = appConfigsRepository.fetchAllByOrgId();
            List<AppConfigItemDTO> items = configs.stream()
                    .map(config -> AppConfigItemDTO.builder()
                            .configKey(config.getConfigKey())
                            .configValue(config.getConfigValue())
                            .configType(config.getConfigType())
                            .description(config.getDescription())
                            .build())
                    .collect(Collectors.toList());
            return new AppConfigsResponseDTO(items);
        }
    }

    /**
     * 버전 문자열 비교 (Semantic Versioning)
     * 예: "1.2.3"을 [1, 2, 3]으로 파싱하여 비교
     *
     * @param version1 비교할 버전 1
     * @param version2 비교할 버전 2
     * @return version1 < version2이면 -1, version1 == version2이면 0, version1 > version2이면 1
     */
    private int compareVersion(String version1, String version2) {
        String[] v1Parts = version1.split("\\.");
        String[] v2Parts = version2.split("\\.");

        int maxLength = Math.max(v1Parts.length, v2Parts.length);
        for (int i = 0; i < maxLength; i++) {
            int v1Part = i < v1Parts.length ? Integer.parseInt(v1Parts[i]) : 0;
            int v2Part = i < v2Parts.length ? Integer.parseInt(v2Parts[i]) : 0;

            if (v1Part < v2Part) {
                return -1;
            } else if (v1Part > v2Part) {
                return 1;
            }
        }
        return 0;
    }
}
