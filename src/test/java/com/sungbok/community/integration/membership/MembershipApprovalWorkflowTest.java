package com.sungbok.community.integration.membership;

import com.sungbok.community.common.constant.UriConstant;
import com.sungbok.community.dto.auth.OAuth2CodeRequest;
import com.sungbok.community.enums.SocialType;
import com.sungbok.community.fixture.OAuthFixture;
import com.sungbok.community.fixture.UserFixture;
import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.service.oauth.impl.GoogleLoginServiceImpl;
import com.sungbok.community.support.BaseIntegrationTest;
import org.jooq.generated.tables.pojos.Memberships;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 멤버십 승인 워크플로우 통합 테스트
 *
 * 테스트 시나리오:
 * 1. OAuth 로그인 → Guest JWT (orgId=null)
 * 2. Guest가 공개 조직 목록 조회
 * 3. Guest가 조직 가입 요청 → PENDING 멤버십 생성
 * 4. 관리자(마을장)가 가입 요청 승인 → APPROVED + 기본 역할 할당
 * 5. 사용자가 JWT refresh → JWT with orgId, roleIds
 * 6. 관리자가 추가 역할 부여 (겸직: 유년부교사, 집사 등)
 * 7. 관리자가 주 역할 변경
 * 8. 관리자가 역할 제거
 *
 * @since 0.0.1
 */
@Transactional
class MembershipApprovalWorkflowTest extends BaseIntegrationTest {

    @MockitoBean
    private GoogleLoginServiceImpl googleLoginService;

    private Long testOrgId;
    private UserMemberDTO adminUser;

    @BeforeEach
    void setup() {
        // Mock Google OAuth Service
        when(googleLoginService.getServiceName()).thenReturn(SocialType.GOOGLE);

        // 테스트 조직 ID 가져오기
        testOrgId = testDataManager.getTestOrgId();

        // 관리자 사용자 생성 (마을장 권한)
        adminUser = UserFixture.builder()
                .email("admin@test.com")
                .name("관리자")
                .build(userRepository, membersRepository, membershipRolesRepository, passwordEncoder);
    }

    @Test
    @DisplayName("전체 워크플로우: OAuth 로그인 → Guest → 가입 요청 → 승인 → 다중 역할 관리")
    void testFullMembershipApprovalWorkflow() throws Exception {
        String guestEmail = "guest@gmail.com";
        String guestName = "게스트 사용자";

        // ===== Step 1: OAuth 로그인 → Guest JWT =====
        OAuthFixture oauthFixture = OAuthFixture.builder()
                .socialType(SocialType.GOOGLE)
                .email(guestEmail)
                .name(guestName)
                .build();
        oauthFixture.setupOAuthServiceMock(googleLoginService, "mock-google-token", "valid-code");

        OAuth2CodeRequest loginRequest = OAuth2CodeRequest.builder()
                .code("valid-code")
                .codeVerifier("mock-verifier")
                .build();

        // OAuth 로그인 (X-Org-Id 없이 Guest JWT 생성)
        MvcResult loginResult = mockMvc.perform(post(UriConstant.AUTH + "/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andReturn();

        String guestAccessToken = tokenTestHelper.extractAccessToken(loginResult);

        // Guest JWT 검증: orgId=null
        assertNull(jwtTokenProvider.getOrgIdFromToken(guestAccessToken),
                "Guest JWT는 orgId가 null이어야 함");
        assertEquals(guestEmail, jwtTokenProvider.getEmailFromToken(guestAccessToken));

        // ===== Step 2: Guest가 공개 조직 목록 조회 =====
        mockMvc.perform(get("/api/organizations")
                        .header("Authorization", "Bearer " + guestAccessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        // ===== Step 3: Guest가 조직 가입 요청 =====
        mockMvc.perform(post("/api/organizations/" + testOrgId + "/join")
                        .header("Authorization", "Bearer " + guestAccessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("가입 요청이 완료되었습니다. 관리자 승인을 기다려주세요."))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        // ===== Step 4: 관리자가 PENDING 멤버십 목록 조회 =====
        String adminAccessToken = jwtTokenProvider.generateAccessToken(adminUser);

        MvcResult pendingResult = mockMvc.perform(get("/api/memberships/pending")
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        // PENDING 멤버십 ID 추출
        String pendingJson = pendingResult.getResponse().getContentAsString();
        List<?> pendingList = objectMapper.readValue(
                objectMapper.readTree(pendingJson).get("data").toString(),
                List.class
        );
        assertTrue(pendingList.size() > 0, "PENDING 멤버십이 있어야 함");

        Map<?, ?> pendingMembership = (Map<?, ?>) pendingList.get(0);
        Long membershipId = ((Number) pendingMembership.get("id")).longValue();

        // ===== Step 5: 관리자가 멤버십 승인 =====
        mockMvc.perform(put("/api/memberships/" + membershipId + "/approve")
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("멤버십이 승인되었습니다."));

        // ===== Step 6: 사용자가 JWT refresh → orgId, roleIds 포함 =====
        String guestRefreshToken = tokenTestHelper.extractRefreshToken(loginResult);

        MvcResult refreshResult = mockMvc.perform(post(UriConstant.AUTH + "/refresh")
                        .header("Authorization", "Bearer " + guestRefreshToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andReturn();

        String newAccessToken = tokenTestHelper.extractAccessToken(refreshResult);

        // JWT 검증: orgId와 roleIds가 포함되어야 함
        assertNotNull(jwtTokenProvider.getOrgIdFromToken(newAccessToken),
                "승인 후 JWT는 orgId가 있어야 함");
        assertEquals(testOrgId, jwtTokenProvider.getOrgIdFromToken(newAccessToken));

        List<Long> roleIds = jwtTokenProvider.getRoleIdsFromToken(newAccessToken);
        assertFalse(roleIds.isEmpty(), "승인 후 기본 역할이 할당되어야 함");

        // ===== Step 7: 관리자가 추가 역할 부여 (겸직) =====
        // 예: 유년부교사 역할 추가
        Long additionalRoleId = testDataManager.getTestRoleId();  // 테스트 역할 ID

        Map<String, Object> addRoleRequest = new HashMap<>();
        addRoleRequest.put("roleId", additionalRoleId);
        addRoleRequest.put("isPrimary", false);

        mockMvc.perform(post("/api/memberships/" + membershipId + "/roles")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRoleRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("역할이 추가되었습니다."));

        // ===== Step 8: 관리자가 주 역할 변경 =====
        Map<String, Object> setPrimaryRequest = new HashMap<>();
        setPrimaryRequest.put("roleId", additionalRoleId);

        mockMvc.perform(put("/api/memberships/" + membershipId + "/roles/primary")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(setPrimaryRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("주 역할이 변경되었습니다."));

        // ===== Step 9: 관리자가 역할 제거 =====
        mockMvc.perform(delete("/api/memberships/" + membershipId + "/roles/" + additionalRoleId)
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("역할이 제거되었습니다."));

        // ===== Step 10: 최종 검증 - 사용자가 자신의 멤버십 조회 =====
        mockMvc.perform(get("/api/memberships/me")
                        .header("Authorization", "Bearer " + newAccessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].status").value("APPROVED"));
    }

    @Test
    @DisplayName("Guest JWT로 플랫폼 API 접근 가능")
    void testGuestJwtCanAccessPlatformApis() throws Exception {
        // Guest JWT 생성 (orgId=null)
        String guestEmail = "guest@example.com";
        OAuthFixture.builder()
                .socialType(SocialType.GOOGLE)
                .email(guestEmail)
                .build()
                .setupOAuthServiceMock(googleLoginService, "mock-token", "valid-code");

        OAuth2CodeRequest loginRequest = OAuth2CodeRequest.builder()
                .code("valid-code")
                .codeVerifier("mock-verifier")
                .build();

        MvcResult loginResult = mockMvc.perform(post(UriConstant.AUTH + "/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String guestAccessToken = tokenTestHelper.extractAccessToken(loginResult);

        // 플랫폼 레벨 API 접근 가능
        mockMvc.perform(get("/api/organizations")
                        .header("Authorization", "Bearer " + guestAccessToken))
                .andDo(print())
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/app-types")
                        .header("Authorization", "Bearer " + guestAccessToken))
                .andDo(print())
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/memberships/me")
                        .header("Authorization", "Bearer " + guestAccessToken))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Guest JWT로 조직 스코프 API 접근 불가 (403)")
    void testGuestJwtCannotAccessOrgScopedApis() throws Exception {
        // Guest JWT 생성
        String guestEmail = "guest-blocked@example.com";
        OAuthFixture.builder()
                .socialType(SocialType.GOOGLE)
                .email(guestEmail)
                .build()
                .setupOAuthServiceMock(googleLoginService, "mock-token", "valid-code");

        OAuth2CodeRequest loginRequest = OAuth2CodeRequest.builder()
                .code("valid-code")
                .codeVerifier("mock-verifier")
                .build();

        MvcResult loginResult = mockMvc.perform(post(UriConstant.AUTH + "/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String guestAccessToken = tokenTestHelper.extractAccessToken(loginResult);

        // 조직 스코프 API는 403 Forbidden
        mockMvc.perform(get("/api/posts")
                        .header("Authorization", "Bearer " + guestAccessToken))
                .andDo(print())
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/memberships/pending")
                        .header("Authorization", "Bearer " + guestAccessToken))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("중복 가입 요청 방지 (동일 사용자가 같은 조직에 재요청)")
    void testDuplicateJoinRequest_ShouldFail() throws Exception {
        String guestEmail = "duplicate-join@gmail.com";

        // Guest JWT 생성
        OAuthFixture.builder()
                .socialType(SocialType.GOOGLE)
                .email(guestEmail)
                .build()
                .setupOAuthServiceMock(googleLoginService, "mock-token", "valid-code");

        OAuth2CodeRequest loginRequest = OAuth2CodeRequest.builder()
                .code("valid-code")
                .codeVerifier("mock-verifier")
                .build();

        MvcResult loginResult = mockMvc.perform(post(UriConstant.AUTH + "/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String guestAccessToken = tokenTestHelper.extractAccessToken(loginResult);

        // 첫 번째 가입 요청 (성공)
        mockMvc.perform(post("/api/organizations/" + testOrgId + "/join")
                        .header("Authorization", "Bearer " + guestAccessToken))
                .andDo(print())
                .andExpect(status().isOk());

        // 두 번째 가입 요청 (실패)
        mockMvc.perform(post("/api/organizations/" + testOrgId + "/join")
                        .header("Authorization", "Bearer " + guestAccessToken))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("이미 해당 조직에")));
    }

    @Test
    @DisplayName("관리자가 멤버십 거절")
    void testAdminRejectsMembership() throws Exception {
        String guestEmail = "rejected@gmail.com";

        // Guest JWT 생성 및 가입 요청
        OAuthFixture.builder()
                .socialType(SocialType.GOOGLE)
                .email(guestEmail)
                .build()
                .setupOAuthServiceMock(googleLoginService, "mock-token", "valid-code");

        OAuth2CodeRequest loginRequest = OAuth2CodeRequest.builder()
                .code("valid-code")
                .codeVerifier("mock-verifier")
                .build();

        MvcResult loginResult = mockMvc.perform(post(UriConstant.AUTH + "/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String guestAccessToken = tokenTestHelper.extractAccessToken(loginResult);

        // 가입 요청
        mockMvc.perform(post("/api/organizations/" + testOrgId + "/join")
                        .header("Authorization", "Bearer " + guestAccessToken))
                .andExpect(status().isOk());

        // 관리자가 PENDING 목록 조회 후 거절
        String adminAccessToken = jwtTokenProvider.generateAccessToken(adminUser);

        MvcResult pendingResult = mockMvc.perform(get("/api/memberships/pending")
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().isOk())
                .andReturn();

        String pendingJson = pendingResult.getResponse().getContentAsString();
        List<?> pendingList = objectMapper.readValue(
                objectMapper.readTree(pendingJson).get("data").toString(),
                List.class
        );

        Long membershipId = ((Number) ((Map<?, ?>) pendingList.get(0)).get("id")).longValue();

        // 멤버십 거절
        mockMvc.perform(put("/api/memberships/" + membershipId + "/reject")
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("멤버십이 거절되었습니다."));

        // 거절된 멤버십 확인
        Memberships rejectedMembership = membersRepository.fetchById(membershipId).orElseThrow();
        assertEquals("REJECTED", rejectedMembership.getStatus());
    }

    @Test
    @DisplayName("다중 역할 관리: 역할 추가 → 주 역할 변경 → 역할 제거")
    void testMultiRoleManagement() throws Exception {
        // APPROVED 멤버십 생성
        UserMemberDTO member = UserFixture.builder()
                .email("multi-role@test.com")
                .name("다중역할 사용자")
                .build(userRepository, membersRepository, membershipRolesRepository, passwordEncoder);

        // 멤버십 ID 조회
        Memberships membership = membersRepository.fetchByUserId(member.getUserId()).orElseThrow();
        Long membershipId = membership.getId();

        String adminAccessToken = jwtTokenProvider.generateAccessToken(adminUser);
        Long roleId2 = testDataManager.getTestRoleId();  // 두 번째 역할

        // 1. 역할 추가 (겸직)
        Map<String, Object> addRoleRequest = new HashMap<>();
        addRoleRequest.put("roleId", roleId2);
        addRoleRequest.put("isPrimary", false);

        mockMvc.perform(post("/api/memberships/" + membershipId + "/roles")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRoleRequest)))
                .andExpect(status().isOk());

        // 2. 주 역할 변경
        Map<String, Object> setPrimaryRequest = new HashMap<>();
        setPrimaryRequest.put("roleId", roleId2);

        mockMvc.perform(put("/api/memberships/" + membershipId + "/roles/primary")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(setPrimaryRequest)))
                .andExpect(status().isOk());

        // 3. 역할 제거
        mockMvc.perform(delete("/api/memberships/" + membershipId + "/roles/" + roleId2)
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().isOk());

        // 검증: 남은 역할이 1개 이상이어야 함
        List<Long> remainingRoles = membershipRolesRepository.fetchRoleIdsByMembershipId(membershipId);
        assertFalse(remainingRoles.isEmpty(), "최소 1개 역할은 남아있어야 함");
    }
}
