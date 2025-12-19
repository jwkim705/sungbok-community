package com.sungbok.community.integration.auth;

import com.sungbok.community.common.constant.UriConstant;
import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.fixture.UserFixture;
import com.sungbok.community.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Disabled;

/**
 * 현재 사용자 정보 조회 통합 테스트
 * GET /auth/me 엔드포인트를 테스트합니다.
 */
class GetCurrentUserIntegrationTest extends BaseIntegrationTest {

    private UserMemberDTO testUser;
    private String validAccessToken;

    @BeforeEach
    void setup() {
        // UserFixture로 테스트 사용자 생성
        testUser = UserFixture.builder()
                .email("me-test@example.com")
                .build(userRepository, membersRepository, membershipRolesRepository, passwordEncoder);

        // JWT 토큰 생성
        validAccessToken = jwtTokenProvider.generateAccessToken(testUser);
    }

    @Test
    @DisplayName("GET /auth/me - 유효한 JWT - 사용자 정보 반환")
    void testGetCurrentUser_WithValidJwt_ShouldReturnUserInfo() throws Exception {
        // When & Then
        mockMvc.perform(get(UriConstant.AUTH + "/me")
                        .header("Authorization", "Bearer " + validAccessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.userId").value(testUser.getUserId()));
    }

    @Test
    @DisplayName("GET /auth/me - JWT 없음 - 400 응답")
    void testGetCurrentUser_WithoutJwt_ShouldReturn400() throws Exception {
        // When & Then: Authorization 헤더 없이 요청
        // Note: /auth/** is permitAll, so request reaches controller but Authentication is null
        mockMvc.perform(get(UriConstant.AUTH + "/me"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("INVALID_FORMAT"))
                .andExpect(jsonPath("$.detail").value("인증 객체는 필수입니다"))
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.code").value("VAL_002"))
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("GET /auth/me - 잘못된 JWT - 400 응답")
    void testGetCurrentUser_WithInvalidJwt_ShouldReturn400() throws Exception {
        // Given: 잘못된 JWT
        String invalidToken = "invalid.jwt.token";

        // When & Then
        // Note: Invalid JWT is filtered out by JwtAuthenticationFilter, but request still reaches controller
        mockMvc.perform(get(UriConstant.AUTH + "/me")
                        .header("Authorization", "Bearer " + invalidToken))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("INVALID_FORMAT"))
                .andExpect(jsonPath("$.detail").value("인증 객체는 필수입니다"))
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.code").value("VAL_002"))
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
