package com.sungbok.community.integration.auth;

import com.sungbok.community.common.constant.UriConstant;
import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.fixture.UserFixture;
import com.sungbok.community.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 로그아웃 통합 테스트
 * POST /auth/logout 엔드포인트를 테스트합니다.
 */
class LogoutIntegrationTest extends BaseIntegrationTest {

    private UserMemberDTO testUser;
    private String validAccessToken;
    private String validRefreshToken;

    @BeforeEach
    void setup() {
        // UserFixture로 테스트 사용자 생성
        testUser = UserFixture.builder()
                .email("logout-test@example.com")
                .build(userRepository, membersRepository, membershipRolesRepository, passwordEncoder);

        // JWT 토큰 생성
        validAccessToken = jwtTokenProvider.generateAccessToken(testUser);
        validRefreshToken = jwtTokenProvider.generateRefreshToken(testUser.getEmail());

        // Redis에 Refresh Token 저장
        refreshTokenService.saveRefreshToken(testUser.getEmail(), validRefreshToken);
    }

    @Test
    @DisplayName("POST /auth/logout - 유효한 JWT - Refresh Token 삭제")
    void testLogout_WithValidAuthentication_ShouldDeleteRefreshToken() throws Exception {
        // When & Then
        mockMvc.perform(post(UriConstant.AUTH + "/logout")
                        .header("Authorization", "Bearer " + validAccessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        // TokenTestHelper로 Redis에서 Refresh Token이 삭제되었는지 확인
        tokenTestHelper.assertRefreshTokenNotInRedis(testUser.getEmail());
    }
}
