package com.sungbok.community.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 앱 동적 설정 응답 DTO
 * 여러 설정 조회 시 사용
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AppConfigsResponseDTO {

    private List<AppConfigItemDTO> configs;
}
