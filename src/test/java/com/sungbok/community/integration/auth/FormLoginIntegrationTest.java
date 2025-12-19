package com.sungbok.community.integration.auth;

import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.fixture.UserFixture;
import com.sungbok.community.support.BaseIntegrationTest;
import com.sungbok.community.support.TokenTestHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Form Login 통합 테스트
 * POST /login 엔드포인트를 통한 이메일/비밀번호 기반 로그인을 테스트합니다.
 *
 * 개선사항:
 * 1. UserFixture 사용으로 테스트 데이터 생성 간소화
 * 2. TokenTestHelper로 토큰 추출/검증 중복 제거
 */
class FormLoginIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Form 로그인 - 유효한 자격 증명 - JWT 토큰 반환")
    void testFormLogin_WithValidCredentials_ShouldReturnJwtTokens() throws Exception {
        // Given: UserFixture로 테스트 사용자 생성
        String email = "formlogin@test.com";
        UserMemberDTO user = UserFixture.builder()
                .email(email)
                .name("Form 로그인 사용자")
                .build(userRepository, membersRepository, membershipRolesRepository, passwordEncoder);

        Long testOrgId = testDataManager.getTestOrgId();

        // When: 유효한 자격 증명으로 로그인
        MvcResult result = mockMvc.perform(post("/login")
                        .header("X-Org-Id", testOrgId)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", email)
                        .param("password", TEST_PASSWORD))
                .andDo(print())
                // Then: 로그인 성공 응답 검증
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.userId").value(user.getUserId()))
                .andExpect(jsonPath("$.tokens.accessToken").exists())
                .andExpect(jsonPath("$.tokens.refreshToken").exists())
                .andExpect(jsonPath("$.tokens.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.tokens.expiresIn").value(900))
                .andReturn();

        // TokenTestHelper로 토큰 추출 (중복 제거)
        TokenTestHelper.TokenPair tokens = tokenTestHelper.extractTokensFromFormLogin(result);

        // TokenTestHelper로 토큰 검증
        tokenTestHelper.assertValidAccessToken(tokens.accessToken(), user);
        tokenTestHelper.assertValidRefreshToken(tokens.refreshToken(), email);

        // Redis에 Refresh Token이 저장되었는지 확인
        tokenTestHelper.assertRefreshTokenStoredInRedis(email, tokens.refreshToken());
    }

    @Test
    @DisplayName("Form 로그인 - 잘못된 비밀번호 - 401 응답")
    void testFormLogin_WithInvalidPassword_ShouldReturn401() throws Exception {
        // Given: UserFixture로 테스트 사용자 생성
        String email = "wrongpwd@test.com";
        UserFixture.builder()
                .email(email)
                .build(userRepository, membersRepository, membershipRolesRepository, passwordEncoder);

        Long testOrgId = testDataManager.getTestOrgId();

        // When & Then: 잘못된 비밀번호로 로그인 시도
        mockMvc.perform(post("/login")
                        .header("X-Org-Id", testOrgId)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", email)
                        .param("password", "WrongPassword123!"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Form 로그인 - 존재하지 않는 이메일 - 401 응답")
    void testFormLogin_WithNonExistentEmail_ShouldReturn401() throws Exception {
        Long testOrgId = testDataManager.getTestOrgId();

        // When & Then: 존재하지 않는 이메일로 로그인 시도
        mockMvc.perform(post("/login")
                        .header("X-Org-Id", testOrgId)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "nonexistent@test.com")
                        .param("password", TEST_PASSWORD))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Form 로그인 - Access Token의 클레임 검증")
    void testFormLogin_TokensShouldContainCorrectClaims() throws Exception {
        // Given: UserFixture로 테스트 사용자 생성
        String email = "claims@test.com";
        UserMemberDTO user = UserFixture.builder()
                .email(email)
                .build(userRepository, membersRepository, membershipRolesRepository, passwordEncoder);

        Long testOrgId = testDataManager.getTestOrgId();

        // When: 로그인
        MvcResult result = mockMvc.perform(post("/login")
                        .header("X-Org-Id", testOrgId)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", email)
                        .param("password", TEST_PASSWORD))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // Then: TokenTestHelper로 Access Token의 클레임 검증
        TokenTestHelper.TokenPair tokens = tokenTestHelper.extractTokensFromFormLogin(result);
        tokenTestHelper.assertValidAccessToken(tokens.accessToken(), user);
    }
}
