package com.sungbok.community.integration.guest;

import com.sungbok.community.support.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comments API Guest Mode 통합 테스트
 * GET /posts/{postId}/comments 엔드포인트를 검증합니다.
 */
class CommentsGuestIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("GET /posts/{postId}/comments - Guest 모드 - 댓글 목록 조회 성공")
    void testGetCommentsByPostId_Guest_ShouldReturn200() throws Exception {
        // Given
        Long orgId = testDataManager.getTestOrgId();
        Long postId = 1L; // 테스트 게시글 (TestDataManager에서 생성)

        // When & Then
        mockMvc.perform(get("/posts/" + postId + "/comments")
                        .header("X-Org-Id", orgId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("댓글 목록 조회 성공"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /posts/{postId}/comments - X-Org-Id 헤더 없음 - 400 에러")
    void testGetCommentsByPostId_NoHeader_ShouldReturn400() throws Exception {
        // Given
        Long postId = 1L;

        // When & Then
        mockMvc.perform(get("/posts/" + postId + "/comments"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
