package com.sungbok.community.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jooq.generated.enums.PlatformType;

/**
 * 앱 버전 업데이트 요청 DTO
 * 관리자가 앱 버전 정보를 업데이트할 때 사용
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAppVersionRequest {

    /**
     * 플랫폼 타입 (ios, android)
     */
    private PlatformType platform;

    /**
     * 최소 지원 버전
     * 예: "1.0.0"
     */
    @NotBlank(message = "최소 버전은 필수입니다")
    private String minVersion;

    /**
     * 최신 버전
     * 예: "1.2.0"
     */
    @NotBlank(message = "최신 버전은 필수입니다")
    private String latestVersion;

    /**
     * 강제 업데이트 메시지
     * 사용자에게 표시될 메시지
     */
    private String forceUpdateMessage;

    /**
     * 업데이트 URL
     * App Store 또는 Google Play Store URL
     */
    private String updateUrl;

    /**
     * 점검 모드 여부
     */
    private Boolean isMaintenance;

    /**
     * 점검 메시지
     */
    private String maintenanceMessage;
}
