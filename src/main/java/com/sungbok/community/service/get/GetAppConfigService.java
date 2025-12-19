package com.sungbok.community.service.get;

import com.sungbok.community.dto.AppConfigItemDTO;
import com.sungbok.community.dto.AppConfigsResponseDTO;
import com.sungbok.community.dto.AppVersionResponseDTO;
import org.jooq.generated.enums.PlatformType;

import java.util.List;

/**
 * 앱 설정 조회 서비스 (CQRS - Query)
 * 앱 버전 체크, 동적 설정 조회
 */
public interface GetAppConfigService {

    /**
     * 앱 버전 체크
     * 현재 버전과 최소/최신 버전을 비교하여 업데이트 필요 여부 반환
     *
     * @param orgId 조직 ID
     * @param platform 플랫폼 타입 (ios, android)
     * @param currentVersion 현재 앱 버전
     * @return 버전 체크 결과 (업데이트 필요 여부, 점검 모드 등)
     */
    AppVersionResponseDTO checkVersion(Long orgId, PlatformType platform, String currentVersion);

    /**
     * 여러 동적 설정 조회
     * Valkey 캐시 우선 조회 후 DB 폴백
     *
     * @param orgId 조직 ID
     * @param configKeys 설정 키 리스트
     * @return 설정 DTO 리스트
     */
    AppConfigsResponseDTO getConfigs(Long orgId, List<String> configKeys);

    /**
     * 단일 동적 설정 조회
     * Valkey 캐시 우선 조회 후 DB 폴백
     *
     * @param orgId 조직 ID
     * @param configKey 설정 키
     * @return 설정 DTO (없으면 null)
     */
    AppConfigItemDTO getConfig(Long orgId, String configKey);

    /**
     * 조직의 모든 동적 설정 조회
     *
     * @param orgId 조직 ID
     * @return 설정 DTO 리스트
     */
    AppConfigsResponseDTO getAllConfigs(Long orgId);
}
