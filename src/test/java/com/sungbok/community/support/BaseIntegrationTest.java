package com.sungbok.community.support;

import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.fixture.UserFixture;
import com.sungbok.community.repository.MembersRepository;
import com.sungbok.community.repository.MembershipRolesRepository;
import com.sungbok.community.repository.UserRepository;
import com.sungbok.community.security.TenantContext;
import com.sungbok.community.security.jwt.JwtTokenProvider;
import com.sungbok.community.security.jwt.RefreshTokenService;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

/**
 * 통합 테스트를 위한 Base 클래스
 * BaseAuthenticationTest를 대체하며 Fixture 패턴 통합
 *
 * 개선사항:
 * 1. Fixture 패턴 통합
 * 2. 헬퍼 메서드 TokenTestHelper로 이동
 * 3. createTestUser() → UserFixture 사용 권장
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    protected static final Logger log = LoggerFactory.getLogger(BaseIntegrationTest.class);

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtTokenProvider jwtTokenProvider;

    @Autowired
    protected RefreshTokenService refreshTokenService;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected MembershipRolesRepository membershipRolesRepository;

    @Autowired
    protected MembersRepository membersRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected StringRedisTemplate redisTemplate;

    @Autowired
    protected DSLContext dsl;

    @Autowired
    protected TestDataManager testDataManager;

    @Autowired
    protected TokenTestHelper tokenTestHelper;

    protected static final String TEST_PASSWORD = "Password123!";

    /**
     * 각 테스트 전에 TenantContext를 초기화합니다.
     * 테스트용 기본 데이터를 생성하고 동적으로 생성된 org_id를 설정합니다.
     */
    @BeforeEach
    void setUpTenantContext() {
        Long orgId = testDataManager.ensureTestDataExists();
        TenantContext.setOrgId(orgId);
    }

    /**
     * 각 테스트 후 Redis에서 refresh token을 정리하고 TenantContext를 정리합니다.
     */
    @AfterEach
    void cleanupRedis() {
        // Redis refresh token 정리
        Set<String> keys = redisTemplate.keys("refresh_token:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        // TenantContext 정리 (ThreadLocal 메모리 누수 방지)
        TenantContext.clear();

        // TestDataManager 정리 (ThreadLocal 메모리 누수 방지)
        testDataManager.clearTestData();
    }

    /**
     * 테스트용 사용자 생성 (레거시 호환용, Fixture 사용 권장)
     *
     * @param email 이메일
     * @return 생성된 사용자 DTO
     * @deprecated UserFixture.builder().build() 사용 권장
     */
    @Deprecated
    protected UserMemberDTO createTestUser(String email) {
        return UserFixture.builder()
                .email(email)
                .build(userRepository, membersRepository, membershipRolesRepository, passwordEncoder);
    }

    /**
     * 테스트용 사용자 생성 (커스텀 역할, 레거시 호환용)
     *
     * @param email   이메일
     * @param roleIds 역할 ID 목록
     * @return 생성된 사용자 DTO
     * @deprecated UserFixture.builder().roleIds(roleIds).build() 사용 권장
     */
    @Deprecated
    protected UserMemberDTO createTestUser(String email, List<Long> roleIds) {
        return UserFixture.builder()
                .email(email)
                .roleIds(roleIds)
                .build(userRepository, membersRepository, membershipRolesRepository, passwordEncoder);
    }
}
