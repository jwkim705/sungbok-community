package com.sungbok.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Push Token 등록 요청 DTO
 * POST /api/notifications/push-tokens 엔드포인트에서 사용
 *
 * @since 0.0.1
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PushTokenRequest {

    /**
     * 기기 고유 식별자 (Valkey 9 Hash Field Expiration 사용)
     * 예: 기기의 UUID, DEVICE_ID, Installation ID 등
     * 기기별 독립적인 TTL 관리를 위해 필수
     */
    @NotBlank(message = "디바이스 ID는 필수입니다")
    @Size(max = 255, message = "디바이스 ID는 255자 이하로 입력해주세요")
    private String deviceId;

    /**
     * Expo Push Token
     * 형식: ExponentPushToken[xxxxxx] 또는 ExpoPushToken[xxxxxx]
     */
    @NotBlank(message = "Expo Push Token은 필수입니다")
    @Pattern(
        regexp = "^(Exponent|Expo)PushToken\\[.+\\]$",
        message = "유효하지 않은 Expo Push Token 형식입니다"
    )
    private String expoPushToken;

    /**
     * 디바이스 타입
     * 예: ios, android
     */
    @Size(max = 50, message = "디바이스 타입은 50자 이하로 입력해주세요")
    private String deviceType;

    /**
     * 디바이스 이름
     * 예: iPhone 14 Pro, Samsung Galaxy S23
     */
    @Size(max = 255, message = "디바이스 이름은 255자 이하로 입력해주세요")
    private String deviceName;

    /**
     * 앱 버전
     * 예: 1.0.0
     */
    @Size(max = 50, message = "앱 버전은 50자 이하로 입력해주세요")
    private String appVersion;
}
