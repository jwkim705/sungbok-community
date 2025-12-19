package com.sungbok.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jooq.generated.enums.ConfigType;

/**
 * 앱 동적 설정 항목 DTO
 * 단일 설정 값 조회 시 사용
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppConfigItemDTO {

    private String configKey;
    private String configValue;
    private ConfigType configType;
    private String description;
}
