package com.sungbok.community.unit.repository;

import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.fixture.UserFixture;
import com.sungbok.community.repository.MembersRepository;
import com.sungbok.community.repository.MembershipRolesRepository;
import com.sungbok.community.repository.UserRepository;
import com.sungbok.community.security.TenantContext;
import com.sungbok.community.support.TestDataManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserRepository 단위 테스트
 * jOOQ Repository 메서드 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembersRepository membersRepository;

    @Autowired
    private MembershipRolesRepository membershipRolesRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TestDataManager testDataManager;

    @BeforeEach
    void setUp() {
        Long orgId = testDataManager.ensureTestDataExists();
        TenantContext.setOrgId(orgId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        testDataManager.clearTestData();
    }

    @Test
    @DisplayName("ID로 사용자 상세 정보 조회")
    void testFetchUserWithDetailsById() {
        // UserFixture 사용
        UserMemberDTO user = UserFixture.builder()
                .email("test@example.com")
                .name("테스트 사용자")
                .build(userRepository, membersRepository, membershipRolesRepository, passwordEncoder);

        // 조회
        Optional<UserMemberDTO> result = userRepository.fetchUserWithDetailsById(user.getUserId());

        // 검증
        assertTrue(result.isPresent(), "사용자가 조회되어야 함");
        assertEquals(user.getEmail(), result.get().getEmail(), "Email이 일치해야 함");
        assertEquals(user.getName(), result.get().getName(), "이름이 일치해야 함");
        assertEquals(user.getUserId(), result.get().getUserId(), "User ID가 일치해야 함");
    }

    @Test
    @DisplayName("이메일로 사용자 상세 정보 조회")
    void testFetchUserWithDetailsByEmail() {
        // UserFixture 사용
        String email = "email-test@example.com";
        UserMemberDTO user = UserFixture.builder()
                .email(email)
                .name("이메일 테스트 사용자")
                .build(userRepository, membersRepository, membershipRolesRepository, passwordEncoder);

        // 조회
        Optional<UserMemberDTO> result = userRepository.fetchUserWithDetailsByEmail(email);

        // 검증
        assertTrue(result.isPresent(), "사용자가 조회되어야 함");
        assertEquals(email, result.get().getEmail(), "Email이 일치해야 함");
        assertEquals(user.getUserId(), result.get().getUserId(), "User ID가 일치해야 함");
        assertEquals(user.getName(), result.get().getName(), "이름이 일치해야 함");
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회 - 빈 Optional 반환")
    void testFetchUserWithDetailsById_NotFound_ShouldReturnEmpty() {
        // 존재하지 않는 ID로 조회
        Optional<UserMemberDTO> result = userRepository.fetchUserWithDetailsById(999999L);

        // 검증
        assertFalse(result.isPresent(), "사용자가 존재하지 않아야 함");
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회 - 빈 Optional 반환")
    void testFetchUserWithDetailsByEmail_NotFound_ShouldReturnEmpty() {
        // 존재하지 않는 이메일로 조회
        Optional<UserMemberDTO> result = userRepository.fetchUserWithDetailsByEmail("nonexistent@example.com");

        // 검증
        assertFalse(result.isPresent(), "사용자가 존재하지 않아야 함");
    }
}
