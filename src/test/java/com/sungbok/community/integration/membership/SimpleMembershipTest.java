package com.sungbok.community.integration.membership;

import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.fixture.UserFixture;
import com.sungbok.community.repository.MembersRepository;
import com.sungbok.community.repository.MembershipRolesRepository;
import com.sungbok.community.repository.RolePermissionsRepository;
import com.sungbok.community.repository.UserRepository;
import com.sungbok.community.security.TenantContext;
import com.sungbok.community.security.jwt.JwtTokenProvider;
import com.sungbok.community.support.BaseIntegrationTest;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 간단한 멤버십 테스트 - 권한 검증
 */
@Transactional
class SimpleMembershipTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DSLContext dsl;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembersRepository membersRepository;

    @Autowired
    private MembershipRolesRepository membershipRolesRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RolePermissionsRepository rolePermissionsRepository;

    private Long testOrgId;
    private UserMemberDTO adminUser;

    @BeforeEach
    void setup() {
        testOrgId = testDataManager.getTestOrgId();
        TenantContext.setOrgId(testOrgId);

        // 관리자 역할 생성
        Long adminRoleId = dsl.insertInto(org.jooq.generated.Tables.ROLES)
                .columns(org.jooq.generated.Tables.ROLES.ORG_ID,
                        org.jooq.generated.Tables.ROLES.NAME,
                        org.jooq.generated.Tables.ROLES.LEVEL,
                        org.jooq.generated.Tables.ROLES.DESCRIPTION)
                .values(testOrgId, "admin", 10, "관리자 역할")
                .returningResult(org.jooq.generated.Tables.ROLES.ID)
                .fetchOne()
                .value1();

        // 관리자 권한 추가
        dsl.insertInto(org.jooq.generated.Tables.ROLE_PERMISSIONS)
                .columns(org.jooq.generated.Tables.ROLE_PERMISSIONS.ROLE_ID,
                        org.jooq.generated.Tables.ROLE_PERMISSIONS.RESOURCE,
                        org.jooq.generated.Tables.ROLE_PERMISSIONS.ACTION,
                        org.jooq.generated.Tables.ROLE_PERMISSIONS.ALLOWED)
                .values(adminRoleId, "users", "read", true)
                .values(adminRoleId, "users", "update", true)
                .values(adminRoleId, "roles", "update", true)
                .execute();

        // 관리자 사용자 생성
        adminUser = UserFixture.builder()
                .email("admin@test.com")
                .name("관리자")
                .roleIds(List.of(adminRoleId))
                .build(userRepository, membersRepository, membershipRolesRepository, passwordEncoder);

        System.out.println("=== Setup Complete ===");
        System.out.println("Admin User: " + adminUser.getEmail());
        System.out.println("Admin Role IDs: " + adminUser.getRoleIds());
        System.out.println("Org ID: " + adminUser.getOrgId());
        System.out.println("=====================");
    }

    @Test
    @DisplayName("관리자 사용자가 제대로 생성되었는지 확인")
    void testAdminUserCreation() {
        assertNotNull(adminUser);
        assertEquals("admin@test.com", adminUser.getEmail());
        assertEquals(testOrgId, adminUser.getOrgId());
        assertFalse(adminUser.getRoleIds().isEmpty(), "관리자 역할이 할당되어야 함");

        // 권한 확인
        Long adminRoleId = adminUser.getRoleIds().get(0);
        var permission = rolePermissionsRepository.fetchByRoleAndResourceAndAction(
                adminRoleId, "users", "read"
        );
        assertTrue(permission.isPresent(), "users.read 권한이 있어야 함");
        assertTrue(permission.get().getAllowed(), "users.read 권한이 허용되어야 함");
    }

    @Test
    @DisplayName("관리자가 PENDING 멤버십 목록을 조회할 수 있는지 확인")
    void testAdminCanAccessPendingMemberships() throws Exception {
        String adminAccessToken = jwtTokenProvider.generateAccessToken(adminUser);

        System.out.println("=== JWT Token ===");
        System.out.println("Token orgId: " + jwtTokenProvider.getOrgIdFromToken(adminAccessToken));
        System.out.println("Token roleIds: " + jwtTokenProvider.getRoleIdsFromToken(adminAccessToken));
        System.out.println("=================");

        mockMvc.perform(get("/api/memberships/pending")
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
