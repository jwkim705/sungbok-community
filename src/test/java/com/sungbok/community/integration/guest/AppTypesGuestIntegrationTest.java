package com.sungbok.community.integration.guest;

import com.sungbok.community.support.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AppTypes API Guest Mode 통합 테스트
 * GET /app-types 엔드포인트를 검증합니다.
 */
class AppTypesGuestIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("GET /app-types - Guest 모드 - 앱 타입 목록 조회 성공")
    void testGetAllAppTypes_Guest_ShouldReturn200() throws Exception {
        // Given
        Long orgId = testDataManager.getTestOrgId();

        // When & Then
        mockMvc.perform(get("/app-types")
                        .header("X-Org-Id", orgId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /app-types/{appTypeId}/organizations - Guest 모드 - 앱 타입별 조직 필터링 성공")
    void testGetOrganizationsByAppType_Guest_ShouldReturn200() throws Exception {
        // Given
        Long orgId = testDataManager.getTestOrgId();
        Long appTypeId = 1L; // Church

        // When & Then
        mockMvc.perform(get("/app-types/" + appTypeId + "/organizations")
                        .header("X-Org-Id", orgId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /app-types - X-Org-Id 헤더 없음 - 플랫폼 레벨 API 허용")
    void testGetAllAppTypes_NoHeader_ShouldReturn200() throws Exception {
        // 플랫폼 레벨 API는 X-Org-Id 헤더 없이도 접근 가능
        // When & Then
        mockMvc.perform(get("/app-types"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
