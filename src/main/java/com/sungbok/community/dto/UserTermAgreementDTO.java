package com.sungbok.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 사용자 약관 동의 이력 DTO
 * 사용자의 약관 동의 기록 조회 시 사용
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTermAgreementDTO {

    /**
     * 동의 이력 ID
     */
    private Long id;

    /**
     * 사용자 ID
     */
    private Long userId;

    /**
     * 약관 ID
     */
    private Long termId;

    /**
     * 약관 제목
     * JOIN을 통해 가져온 약관 정보
     */
    private String termTitle;

    /**
     * 약관 버전
     * JOIN을 통해 가져온 약관 정보
     */
    private String termVersion;

    /**
     * 동의 일시
     */
    private LocalDateTime agreedAt;

    /**
     * 동의 시 IP 주소
     * 법적 증거로 사용
     */
    private String ipAddress;
}
