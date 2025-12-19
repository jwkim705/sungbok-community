package com.sungbok.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 앱 버전 체크 응답 DTO
 * 클라이언트가 앱 버전을 체크할 때 반환되는 정보
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppVersionResponseDTO {

    /**
     * 업데이트 필요 여부
     * 현재 버전이 최소 버전보다 낮으면 true
     */
    private Boolean needsUpdate;

    /**
     * 강제 업데이트 여부
     * 현재 버전이 최소 버전보다 낮으면 true (앱 사용 불가)
     */
    private Boolean forceUpdate;

    /**
     * 최신 버전
     * 예: "1.2.0"
     */
    private String latestVersion;

    /**
     * 업데이트 메시지
     * 사용자에게 표시될 메시지
     */
    private String message;

    /**
     * 업데이트 URL
     * App Store 또는 Google Play Store URL
     */
    private String updateUrl;

    /**
     * 점검 모드 여부
     * true이면 앱 사용 불가
     */
    private Boolean isMaintenance;

    /**
     * 점검 메시지
     * 점검 모드일 때 사용자에게 표시될 메시지
     */
    private String maintenanceMessage;
}
