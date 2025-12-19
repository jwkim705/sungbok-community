package com.sungbok.community.service.change;

import com.sungbok.community.dto.UpdateAppConfigRequest;
import com.sungbok.community.dto.UpdateAppVersionRequest;
import org.jooq.generated.enums.PlatformType;

/**
 * 앱 설정 변경 서비스 (CQRS - Command)
 * 앱 버전 업데이트, 동적 설정 변경 (관리자 전용)
 */
public interface ChangeAppConfigService {

    /**
     * 앱 버전 정보 업데이트
     * 관리자 전용 - 최소/최신 버전 변경
     *
     * @param orgId 조직 ID
     * @param request 버전 정보 (minVersion, latestVersion, updateUrl 등)
     */
    void updateVersion(Long orgId, UpdateAppVersionRequest request);

    /**
     * 점검 모드 설정
     * 관리자 전용 - 앱 점검 모드 on/off
     *
     * @param orgId 조직 ID
     * @param platform 플랫폼 타입 (ios, android)
     * @param isMaintenance 점검 모드 여부
     * @param message 점검 메시지
     */
    void updateMaintenanceMode(Long orgId, PlatformType platform, boolean isMaintenance, String message);

    /**
     * 동적 설정 변경 (Upsert)
     * 관리자 전용 - 조직별 동적 설정 추가/수정
     * Valkey 캐시 무효화
     *
     * @param orgId 조직 ID
     * @param request 설정 정보 (configKey, configValue, configType 등)
     */
    void upsertConfig(Long orgId, UpdateAppConfigRequest request);

    /**
     * 동적 설정 삭제
     * 관리자 전용
     * Valkey 캐시 무효화
     *
     * @param orgId 조직 ID
     * @param configKey 설정 키
     */
    void deleteConfig(Long orgId, String configKey);
}
