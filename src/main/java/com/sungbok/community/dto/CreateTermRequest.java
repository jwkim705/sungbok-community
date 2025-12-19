package com.sungbok.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jooq.generated.enums.TermType;

import java.time.LocalDate;

/**
 * 약관 생성 요청 DTO
 * 관리자가 새로운 약관 버전을 생성할 때 사용
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTermRequest {

    /**
     * 약관 타입
     * TOS(이용약관), PRIVACY(개인정보처리방침), MARKETING(마케팅동의)
     */
    @NotNull(message = "약관 타입은 필수입니다")
    private TermType termType;

    /**
     * 약관 버전
     * 예: "1.0", "1.1", "2.0"
     */
    @NotBlank(message = "약관 버전은 필수입니다")
    private String version;

    /**
     * 약관 제목
     */
    @NotBlank(message = "약관 제목은 필수입니다")
    private String title;

    /**
     * 약관 내용
     * HTML 또는 Markdown 형식
     */
    @NotBlank(message = "약관 내용은 필수입니다")
    private String content;

    /**
     * 필수 동의 여부
     * true: 필수 약관, false: 선택 약관
     */
    @NotNull(message = "필수 동의 여부는 필수입니다")
    private Boolean isRequired;

    /**
     * 약관 발효일
     */
    @NotNull(message = "약관 발효일은 필수입니다")
    private LocalDate effectiveDate;
}
