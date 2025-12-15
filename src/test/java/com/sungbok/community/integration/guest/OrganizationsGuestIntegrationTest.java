package com.sungbok.community.integration.guest;

import com.sungbok.community.support.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Organizations API Guest Mode 통합 테스트
 * GET /organizations 엔드포인트를 검증합니다.
 */
class OrganizationsGuestIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("GET /organizations - Guest 모드 - 공개 조직 목록 조회 성공")
    void testGetPublicOrganizations_Guest_ShouldReturn200() throws Exception {
        // Given: 테스트 데이터는 BaseIntegrationTest에서 자동 생성됨
        Long orgId = testDataManager.getTestOrgId();

        // When & Then
        mockMvc.perform(get("/organizations")
                        .header("X-Org-Id", orgId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("공개 조직 목록 조회 성공"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /organizations/{orgId} - Guest 모드 - 공개 조직 상세 조회 성공")
    void testGetOrganizationById_PublicOrg_ShouldReturn200() throws Exception {
        // Given
        Long orgId = testDataManager.getTestOrgId();

        // When & Then
        mockMvc.perform(get("/organizations/" + orgId)
                        .header("X-Org-Id", orgId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("조직 조회 성공"))
                .andExpect(jsonPath("$.data.orgId").value(orgId));
    }

    @Test
    @DisplayName("GET /organizations - X-Org-Id 헤더 없음 - 플랫폼 레벨 API 허용")
    void testGetPublicOrganizations_NoHeader_ShouldReturn200() throws Exception {
        // 플랫폼 레벨 API는 X-Org-Id 헤더 없이도 접근 가능
        // When & Then
        mockMvc.perform(get("/organizations"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("공개 조직 목록 조회 성공"));
    }

    @Test
    @DisplayName("GET /organizations/{orgId} - 존재하지 않는 조직 - 404 에러")
    void testGetOrganizationById_NotExist_ShouldReturn404() throws Exception {
        // Given
        Long orgId = testDataManager.getTestOrgId();
        Long nonExistentOrgId = 99999L;

        // When & Then
        mockMvc.perform(get("/organizations/" + nonExistentOrgId)
                        .header("X-Org-Id", orgId))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
}
