package com.sungbok.community.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jooq.generated.enums.ConfigType;

/**
 * 앱 설정 업데이트 요청 DTO
 * 조직별 동적 설정을 변경할 때 사용
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAppConfigRequest {

    /**
     * 설정 키
     * 예: theme_primary_color, logo_url, welcome_message
     */
    @NotBlank(message = "설정 키는 필수입니다")
    private String configKey;

    /**
     * 설정 값
     * 타입에 따라 다양한 형식 지원 (문자열, 숫자, JSON 등)
     */
    @NotBlank(message = "설정 값은 필수입니다")
    private String configValue;

    /**
     * 설정 값 타입
     * string, integer, float, double, boolean, json, array, date
     */
    private ConfigType configType;

    /**
     * 설정 설명
     */
    private String description;
}
