package com.sungbok.community.integration.auth;

import com.sungbok.community.common.constant.UriConstant;
import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.dto.auth.OAuth2CodeRequest;
import com.sungbok.community.enums.SocialType;
import com.sungbok.community.fixture.OAuthFixture;
import com.sungbok.community.fixture.UserFixture;
import com.sungbok.community.service.oauth.impl.GoogleLoginServiceImpl;
import com.sungbok.community.service.oauth.impl.KakaoLoginServiceImpl;
import com.sungbok.community.service.oauth.impl.NaverLoginServiceImpl;
import com.sungbok.community.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OAuth2 로그인 통합 테스트
 * Flow 2 (앱 주도 OAuth 2.1)를 테스트합니다.
 * POST /auth/login/{provider} 엔드포인트를 검증합니다.
 *
 * 개선사항:
 * 1. OAuthFixture로 Mock 데이터 생성 중복 제거
 * 2. TokenTestHelper로 토큰 추출 중복 제거
 * 3. UserFixture로 테스트 사용자 생성
 */
@Transactional
class OAuth2LoginIntegrationTest extends BaseIntegrationTest {

    @MockitoBean
    private GoogleLoginServiceImpl googleLoginService;

    @MockitoBean
    private KakaoLoginServiceImpl kakaoLoginService;

    @MockitoBean
    private NaverLoginServiceImpl naverLoginService;

    @BeforeEach
    void setupMocks() {
        // Mock service name methods
        when(googleLoginService.getServiceName()).thenReturn(SocialType.GOOGLE);
        when(kakaoLoginService.getServiceName()).thenReturn(SocialType.KAKAO);
        when(naverLoginService.getServiceName()).thenReturn(SocialType.NAVER);
    }

    @Test
    @DisplayName("Google 로그인 - 유효한 Authorization Code - JWT 반환")
    void testGoogleLogin_ValidCode_ShouldReturnJwt() throws Exception {
        // OAuthFixture로 Mock 데이터 생성 (중복 제거)
        String email = "google-user@gmail.com";
        OAuthFixture oauthFixture = OAuthFixture.builder()
                .socialType(SocialType.GOOGLE)
                .email(email)
                .build();

        // OAuth Service Mock 설정 (중복 제거)
        oauthFixture.setupOAuthServiceMock(googleLoginService, "mock-google-access-token", "valid-google-code");

        OAuth2CodeRequest request = OAuth2CodeRequest.builder()
                .code("valid-google-code")
                .codeVerifier("mock-verifier")
                .build();

        // 실행 & 검증
        Long orgId = testDataManager.getTestOrgId();
        MvcResult result = mockMvc.perform(post(UriConstant.AUTH + "/login/google")
                        .header("X-Org-Id", orgId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(900))
                .andReturn();

        // TokenTestHelper로 JWT 검증 (중복 제거)
        String accessToken = tokenTestHelper.extractAccessToken(result);
        assertTrue(jwtTokenProvider.validateToken(accessToken));
        assertEquals(email, jwtTokenProvider.getEmailFromToken(accessToken));
    }

    @Test
    @DisplayName("Kakao 로그인 - 유효한 Authorization Code - 기존 사용자 JWT 반환")
    void testKakaoLogin_ValidCode_ExistingUser_ShouldReturnJwt() throws Exception {
        // UserFixture로 기존 사용자 생성
        String email = "kakao-user@kakao.com";
        UserMemberDTO existingUser = UserFixture.builder()
                .email(email)
                .name("Kakao 사용자")
                .build(userRepository, membersRepository, membershipRolesRepository, passwordEncoder);

        // OAuthFixture로 Kakao OAuth Mock
        OAuthFixture.builder()
                .socialType(SocialType.KAKAO)
                .email(email)
                .build()
                .setupOAuthServiceMock(kakaoLoginService, "mock-kakao-access-token", "valid-kakao-code");

        OAuth2CodeRequest request = OAuth2CodeRequest.builder()
                .code("valid-kakao-code")
                .build();  // codeVerifier 없음 (Kakao는 PKCE 미지원)

        // 실행 & 검증
        Long orgId = testDataManager.getTestOrgId();
        MvcResult result = mockMvc.perform(post(UriConstant.AUTH + "/login/kakao")
                        .header("X-Org-Id", orgId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andReturn();

        // JWT 검증 - 기존 사용자 정보와 일치해야 함
        String accessToken = tokenTestHelper.extractAccessToken(result);
        assertEquals(existingUser.getEmail(), jwtTokenProvider.getEmailFromToken(accessToken));
        assertEquals(existingUser.getUserId(), jwtTokenProvider.getUserIdFromToken(accessToken));
    }

    @Test
    @DisplayName("Naver 로그인 - 유효한 Authorization Code - JWT 반환")
    void testNaverLogin_ValidCode_ShouldReturnJwt() throws Exception {
        // OAuthFixture로 Naver OAuth Mock
        String email = "naver-user@naver.com";
        OAuthFixture.builder()
                .socialType(SocialType.NAVER)
                .email(email)
                .build()
                .setupOAuthServiceMock(naverLoginService, "mock-naver-access-token", "valid-naver-code");

        OAuth2CodeRequest request = OAuth2CodeRequest.builder()
                .code("valid-naver-code")
                .build();

        // 실행 & 검증
        Long orgId = testDataManager.getTestOrgId();
        MvcResult result = mockMvc.perform(post(UriConstant.AUTH + "/login/naver")
                        .header("X-Org-Id", orgId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andReturn();

        // JWT 검증
        String accessToken = tokenTestHelper.extractAccessToken(result);
        assertTrue(jwtTokenProvider.validateToken(accessToken));
        assertEquals(email, jwtTokenProvider.getEmailFromToken(accessToken));
    }

    @Test
    @DisplayName("Google 로그인 - 잘못된 Authorization Code - 500 응답")
    void testGoogleLogin_InvalidCode_ShouldReturn500() throws Exception {
        // Mock 에러 응답
        when(googleLoginService.getSocialAccessToken(eq("invalid-code"), anyString()))
                .thenThrow(new RuntimeException("Failed to exchange Google authorization code"));

        OAuth2CodeRequest request = OAuth2CodeRequest.builder()
                .code("invalid-code")
                .codeVerifier("mock-verifier")
                .build();

        // 실행 & 검증
        mockMvc.perform(post(UriConstant.AUTH + "/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().is5xxServerError());  // RuntimeException → 500
    }

    @Test
    @DisplayName("OAuth 로그인 - 지원하지 않는 Provider - 400 응답")
    void testOAuthLogin_UnsupportedProvider_ShouldReturn400() throws Exception {
        OAuth2CodeRequest request = OAuth2CodeRequest.builder()
                .code("valid-code")
                .codeVerifier("mock-verifier")
                .build();

        // 실행 & 검증
        mockMvc.perform(post(UriConstant.AUTH + "/login/facebook")  // 미지원 provider
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("OAuth 로그인 - Authorization Code 누락 - 400 응답")
    void testOAuthLogin_MissingCode_ShouldReturn400() throws Exception {
        // code 없음
        OAuth2CodeRequest request = OAuth2CodeRequest.builder()
                .codeVerifier("mock-verifier")
                .build();

        // 실행 & 검증
        mockMvc.perform(post(UriConstant.AUTH + "/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("OAuth 로그인 - 동일 이메일 다른 Provider - OAuth 계정 추가 등록")
    void testOAuthLogin_SameEmailDifferentProvider_ShouldLinkAccount() throws Exception {
        // UserFixture로 기존 사용자 생성 (Google로 가입)
        String email = "duplicate@example.com";
        UserFixture.builder()
                .email(email)
                .build(userRepository, membersRepository, membershipRolesRepository, passwordEncoder);

        // OAuthFixture로 Kakao로 동일 이메일 로그인 시도
        OAuthFixture.builder()
                .socialType(SocialType.KAKAO)
                .email(email)
                .build()
                .setupOAuthServiceMock(kakaoLoginService, "mock-kakao-token", "valid-kakao-code");

        OAuth2CodeRequest request = OAuth2CodeRequest.builder()
                .code("valid-kakao-code")
                .build();

        // 실행 & 검증 - 로그인 성공해야 함
        Long orgId = testDataManager.getTestOrgId();
        mockMvc.perform(post(UriConstant.AUTH + "/login/kakao")
                        .header("X-Org-Id", orgId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    @DisplayName("OAuth 로그인 성공 - Refresh Token이 Redis에 저장")
    void testOAuthLogin_Success_RefreshTokenStoredInRedis() throws Exception {
        // OAuthFixture로 Google OAuth Mock
        String email = "redis-test@gmail.com";
        OAuthFixture.builder()
                .socialType(SocialType.GOOGLE)
                .email(email)
                .build()
                .setupOAuthServiceMock(googleLoginService, "mock-access-token", "valid-code");

        OAuth2CodeRequest request = OAuth2CodeRequest.builder()
                .code("valid-code")
                .codeVerifier("verifier")
                .build();

        // 실행
        Long orgId = testDataManager.getTestOrgId();
        MvcResult result = mockMvc.perform(post(UriConstant.AUTH + "/login/google")
                        .header("X-Org-Id", orgId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // TokenTestHelper로 검증: Redis에 Refresh Token 저장 확인
        String refreshToken = tokenTestHelper.extractRefreshToken(result);
        tokenTestHelper.assertRefreshTokenStoredInRedis(email, refreshToken);
    }
}
