package com.sungbok.community.security;

import tools.jackson.databind.JsonNode;
import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Form Login 인증 테스트
 * POST /login 엔드포인트를 통한 이메일/비밀번호 기반 로그인을 테스트합니다.
 */
class FormLoginAuthenticationTest extends BaseAuthenticationTest {

    @Test
    @DisplayName("Form Login - Valid Credentials - Should Return JWT Tokens")
    void testFormLogin_WithValidCredentials_ShouldReturnJwtTokens() throws Exception {
        // Given: 테스트 사용자 생성
        String email = "formlogin@test.com";
        UserMemberDTO user = createTestUser(email, TEST_PASSWORD, UserRole.USER);

        // When: 유효한 자격 증명으로 로그인
        MvcResult result = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", email)
                        .param("password", TEST_PASSWORD))
                .andDo(print())
                // Then: 로그인 성공 응답 검증
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.user.email").value(email))
                .andExpect(jsonPath("$.data.user.userId").value(user.getUserId()))
                .andExpect(jsonPath("$.data.tokens.accessToken").exists())
                .andExpect(jsonPath("$.data.tokens.refreshToken").exists())
                .andExpect(jsonPath("$.data.tokens.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.tokens.expiresIn").value(900))
                .andReturn();

        // 토큰 추출 및 검증
        String responseBody = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(responseBody);
        String accessToken = root.path("data").path("tokens").path("accessToken").asText();
        String refreshToken = root.path("data").path("tokens").path("refreshToken").asText();

        // Access Token 검증
        assertValidAccessToken(accessToken, user);

        // Refresh Token 검증
        assertValidRefreshToken(refreshToken, email);

        // Redis에 Refresh Token이 저장되었는지 확인
        assertRefreshTokenStoredInRedis(email, refreshToken);
    }

    @Test
    @DisplayName("Form Login - Invalid Password - Should Return 401")
    void testFormLogin_WithInvalidPassword_ShouldReturn401() throws Exception {
        // Given: 테스트 사용자 생성
        String email = "wrongpwd@test.com";
        createTestUser(email, TEST_PASSWORD, UserRole.USER);

        // When & Then: 잘못된 비밀번호로 로그인 시도
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", email)
                        .param("password", "WrongPassword123!"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Form Login - Non-Existent Email - Should Return 401")
    void testFormLogin_WithNonExistentEmail_ShouldReturn401() throws Exception {
        // When & Then: 존재하지 않는 이메일로 로그인 시도
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "nonexistent@test.com")
                        .param("password", TEST_PASSWORD))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Form Login - Access Token Should Contain Correct Claims")
    void testFormLogin_TokensShouldContainCorrectClaims() throws Exception {
        // Given: 테스트 사용자 생성
        String email = "claims@test.com";
        UserMemberDTO user = createTestUser(email, TEST_PASSWORD, UserRole.USER);

        // When: 로그인
        MvcResult result = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", email)
                        .param("password", TEST_PASSWORD))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // Then: Access Token의 클레임 검증
        String responseBody = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(responseBody);
        String accessToken = root.path("data").path("tokens").path("accessToken").asText();

        // JWT 클레임 검증
        assertEquals(email, jwtTokenProvider.getEmailFromToken(accessToken),
                "Access token should contain correct email");
        assertEquals(user.getUserId(), jwtTokenProvider.getUserIdFromToken(accessToken),
                "Access token should contain correct user ID");
        assertEquals(user.getRole().getCode(), jwtTokenProvider.getRoleFromToken(accessToken),
                "Access token should contain correct role");
    }
}
