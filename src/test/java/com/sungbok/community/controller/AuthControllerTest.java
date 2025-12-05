package com.sungbok.community.controller;

import tools.jackson.databind.JsonNode;
import com.sungbok.community.common.constant.UriConstant;
import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.dto.auth.RefreshTokenRequest;
import com.sungbok.community.enums.UserRole;
import com.sungbok.community.security.BaseAuthenticationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 테스트
 * JWT 토큰 관리 엔드포인트(refresh, logout, /me)를 테스트합니다.
 */
class AuthControllerTest extends BaseAuthenticationTest {

    private UserMemberDTO testUser;
    private String validAccessToken;
    private String validRefreshToken;

    @BeforeEach
    void setup() {
        // 테스트 사용자 생성
        testUser = createTestUser("authtest@example.com", TEST_PASSWORD, UserRole.USER);

        // JWT 토큰 생성
        validAccessToken = jwtTokenProvider.generateAccessToken(testUser);
        validRefreshToken = jwtTokenProvider.generateRefreshToken(testUser.getEmail());

        // Redis에 Refresh Token 저장
        refreshTokenService.saveRefreshToken(testUser.getEmail(), validRefreshToken);
    }

    @Test
    @DisplayName("POST /auth/refresh - Valid Refresh Token - Should Return New Access Token")
    void testRefreshToken_WithValidRefreshToken_ShouldReturnNewAccessToken() throws Exception {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest(validRefreshToken);

        // When & Then
        MvcResult result = mockMvc.perform(post(UriConstant.AUTH + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").value(validRefreshToken))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(900))
                .andReturn();

        // 새로운 Access Token 검증
        String responseBody = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(responseBody);
        String newAccessToken = root.path("data").path("accessToken").asText();

        assertTrue(jwtTokenProvider.validateToken(newAccessToken),
                "New access token should be valid");
        assertEquals(testUser.getEmail(), jwtTokenProvider.getEmailFromToken(newAccessToken),
                "New access token should contain correct email");
    }

    @Test
    @DisplayName("POST /auth/refresh - Invalid Refresh Token - Should Return 401")
    void testRefreshToken_WithInvalidRefreshToken_ShouldReturn401() throws Exception {
        // Given: 잘못된 refresh token
        RefreshTokenRequest request = new RefreshTokenRequest("invalid.token.here");

        // When & Then
        mockMvc.perform(post(UriConstant.AUTH + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"));
    }

    @Test
    @DisplayName("POST /auth/logout - Valid JWT - Should Delete Refresh Token")
    void testLogout_WithValidAuthentication_ShouldDeleteRefreshToken() throws Exception {
        // When & Then
        mockMvc.perform(post(UriConstant.AUTH + "/logout")
                        .header("Authorization", "Bearer " + validAccessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        // Redis에서 Refresh Token이 삭제되었는지 확인
        assertRefreshTokenNotInRedis(testUser.getEmail());
    }

    @Test
    @DisplayName("GET /auth/me - Valid JWT - Should Return User Info")
    void testGetCurrentUser_WithValidJwt_ShouldReturnUserInfo() throws Exception {
        // When & Then
        mockMvc.perform(get(UriConstant.AUTH + "/me")
                        .header("Authorization", "Bearer " + validAccessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("User info retrieved"))
                .andExpect(jsonPath("$.data.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.data.userId").value(testUser.getUserId()))
                .andExpect(jsonPath("$.data.role").value(testUser.getRole().getCode()));
    }

    @Test
    @DisplayName("GET /auth/me - No JWT - Should Return 400")
    void testGetCurrentUser_WithoutJwt_ShouldReturn400() throws Exception {
        // When & Then: Authorization 헤더 없이 요청
        // Note: /auth/** is permitAll, so request reaches controller but Authentication is null
        mockMvc.perform(get(UriConstant.AUTH + "/me"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Authentication must not be null"));
    }

    @Test
    @DisplayName("GET /auth/me - Invalid JWT - Should Return 400")
    void testGetCurrentUser_WithInvalidJwt_ShouldReturn400() throws Exception {
        // Given: 잘못된 JWT
        String invalidToken = "invalid.jwt.token";

        // When & Then
        // Note: Invalid JWT is filtered out by JwtAuthenticationFilter, but request still reaches controller
        mockMvc.perform(get(UriConstant.AUTH + "/me")
                        .header("Authorization", "Bearer " + invalidToken))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
