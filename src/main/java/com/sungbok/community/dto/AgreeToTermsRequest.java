package com.sungbok.community.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 약관 동의 요청 DTO
 * 사용자가 여러 약관에 동의할 때 사용 (회원가입 시 Batch INSERT)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgreeToTermsRequest {

    /**
     * 동의할 약관 ID 리스트
     * 회원가입 시 여러 약관에 한 번에 동의
     */
    @NotEmpty(message = "동의할 약관 ID는 최소 1개 이상이어야 합니다")
    private List<Long> termIds;
}
