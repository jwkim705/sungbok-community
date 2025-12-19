package com.sungbok.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jooq.generated.enums.TermType;

import java.time.LocalDate;

/**
 * 약관 정보 DTO
 * 현재 유효한 약관 정보를 조회할 때 사용
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TermDTO {

    /**
     * 약관 ID
     */
    private Long id;

    /**
     * 조직 ID
     * NULL이면 플랫폼 전체 약관
     */
    private Long orgId;

    /**
     * 약관 타입
     * TOS(이용약관), PRIVACY(개인정보처리방침), MARKETING(마케팅동의)
     */
    private TermType termType;

    /**
     * 약관 버전
     * 예: "1.0", "1.1", "2.0"
     */
    private String version;

    /**
     * 약관 제목
     */
    private String title;

    /**
     * 약관 내용
     * HTML 또는 Markdown 형식
     */
    private String content;

    /**
     * 필수 동의 여부
     * true: 필수 약관, false: 선택 약관
     */
    private Boolean isRequired;

    /**
     * 현재 유효한 약관 여부
     * true이면 현재 사용 중인 버전
     */
    private Boolean isCurrent;

    /**
     * 약관 발효일
     */
    private LocalDate effectiveDate;
}
