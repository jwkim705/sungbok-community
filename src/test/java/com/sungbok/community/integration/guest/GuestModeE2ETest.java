package com.sungbok.community.integration.guest;

import com.sungbok.community.support.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guest Mode End-to-End 시나리오 테스트
 * 모바일 앱에서 Guest 모드로 조직 미리보기 플로우를 검증합니다.
 */
class GuestModeE2ETest extends BaseIntegrationTest {

    @Test
    @DisplayName("Guest 모드 전체 플로우 - 조직 선택 → 게시글 조회 → 로그인 필요")
    void testGuestModeFullFlow() throws Exception {
        // 1. 조직 목록 조회 (X-Org-Id 없이) - 플랫폼 레벨 API는 허용
        mockMvc.perform(get("/organizations"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        // 2. Guest가 조직 선택 후 조직 상세 조회
        Long orgId = testDataManager.getTestOrgId();
        mockMvc.perform(get("/organizations/" + orgId)
                        .header("X-Org-Id", orgId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orgId").value(orgId));

        // 3. Guest가 선택한 조직의 게시글 조회
        mockMvc.perform(get("/posts")
                        .header("X-Org-Id", orgId)
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "createdAt")
                        .param("direction", "desc"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        // 4. Guest가 게시글 작성 시도 → 401 Unauthorized
        mockMvc.perform(post("/posts")
                        .header("X-Org-Id", orgId)
                        .contentType("application/json")
                        .content("{\"title\":\"Test\",\"content\":\"Test\"}"))
                .andDo(print())
                .andExpect(status().isUnauthorized()); // 쓰기는 인증 필요 (401)
    }

    @Test
    @DisplayName("Guest 모드 - 비공개 조직 접근 시도 - 400 에러")
    void testGuestMode_PrivateOrg_ShouldReturn400() throws Exception {
        // Given: 비공개 조직 (is_public=false)
        Long privateOrgId = 99999L; // 존재하지 않거나 비공개

        // When & Then
        mockMvc.perform(get("/posts")
                        .header("X-Org-Id", privateOrgId))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Guest 모드 - 잘못된 X-Org-Id 형식 - 400 에러")
    void testGuestMode_InvalidOrgIdFormat_ShouldReturn400() throws Exception {
        // When & Then
        mockMvc.perform(get("/posts")
                        .header("X-Org-Id", "invalid"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
