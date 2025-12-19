package com.sungbok.community.integration.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sungbok.community.common.constant.UriConstant;
import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.dto.auth.RefreshTokenRequest;
import com.sungbok.community.fixture.UserFixture;
import com.sungbok.community.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import org.junit.jupiter.api.Disabled;

/**
 * JWT Refresh Token 통합 테스트
 * POST /auth/refresh 엔드포인트를 테스트합니다.
 */
class TokenRefreshIntegrationTest extends BaseIntegrationTest {

    private UserMemberDTO testUser;
    private String validRefreshToken;

    @BeforeEach
    void setup() {
        // UserFixture로 테스트 사용자 생성
        testUser = UserFixture.builder()
                .email("authtest@example.com")
                .build(userRepository, membersRepository, membershipRolesRepository, passwordEncoder);

        // JWT 토큰 생성
        validRefreshToken = jwtTokenProvider.generateRefreshToken(testUser.getEmail());

        // Redis에 Refresh Token 저장
        refreshTokenService.saveRefreshToken(testUser.getEmail(), validRefreshToken);
    }

    @Test
    @DisplayName("POST /auth/refresh - 유효한 Refresh Token - 새 Access Token 반환")
    void testRefreshToken_WithValidRefreshToken_ShouldReturnNewAccessToken() throws Exception {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest(validRefreshToken);

        // When & Then
        MvcResult result = mockMvc.perform(post(UriConstant.AUTH + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").value(validRefreshToken))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andReturn();

        // TokenTestHelper로 새로운 Access Token 검증
        String newAccessToken = tokenTestHelper.extractAccessToken(result);
        assertTrue(jwtTokenProvider.validateToken(newAccessToken),
                "새 Access token이 유효해야 함");
        assertEquals(testUser.getEmail(), jwtTokenProvider.getEmailFromToken(newAccessToken),
                "새 Access token이 올바른 email을 포함해야 함");
    }

    @Test
    @DisplayName("POST /auth/refresh - 잘못된 Refresh Token - 401 응답")
    void testRefreshToken_WithInvalidRefreshToken_ShouldReturn401() throws Exception {
        // Given: 잘못된 refresh token
        RefreshTokenRequest request = new RefreshTokenRequest("invalid.token.here");

        // When & Then
        mockMvc.perform(post(UriConstant.AUTH + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
