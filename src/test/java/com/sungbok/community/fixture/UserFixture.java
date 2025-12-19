package com.sungbok.community.fixture;

import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.repository.MembersRepository;
import com.sungbok.community.repository.MembershipRolesRepository;
import com.sungbok.community.repository.UserRepository;
import com.sungbok.community.security.TenantContext;
import com.sungbok.community.support.TestDataManager;
import org.jooq.generated.enums.MembershipStatus;
import org.jooq.generated.tables.pojos.Memberships;
import org.jooq.generated.tables.pojos.Users;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 테스트 데이터 생성 Fixture (빌더 패턴)
 *
 * 사용 예시:
 * <pre>
 * UserMemberDTO user = UserFixture.builder()
 *     .email("test@example.com")
 *     .name("테스트 사용자")
 *     .roleIds(List.of(1L, 2L))
 *     .build(userRepository, membersRepository, membershipRolesRepository, passwordEncoder);
 * </pre>
 */
public class UserFixture {

    private String email = "default@test.com";
    private String password = "Password123!";
    private String name = "테스트 사용자";
    private List<Long> roleIds = List.of();  // 빈 리스트 = 기본 역할 사용
    private MembershipStatus status = MembershipStatus.APPROVED;

    /**
     * 빌더 시작
     *
     * @return UserFixture 인스턴스
     */
    public static UserFixture builder() {
        return new UserFixture();
    }

    /**
     * 이메일 설정
     *
     * @param email 이메일
     * @return this
     */
    public UserFixture email(String email) {
        this.email = email;
        return this;
    }

    /**
     * 비밀번호 설정
     *
     * @param password 비밀번호
     * @return this
     */
    public UserFixture password(String password) {
        this.password = password;
        return this;
    }

    /**
     * 이름 설정
     *
     * @param name 이름
     * @return this
     */
    public UserFixture name(String name) {
        this.name = name;
        return this;
    }

    /**
     * 역할 ID 목록 설정
     *
     * @param roleIds 역할 ID 목록
     * @return this
     */
    public UserFixture roleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
        return this;
    }

    /**
     * 상태 설정
     *
     * @param status 상태 (예: APPROVED, PENDING)
     * @return this
     */
    public UserFixture status(MembershipStatus status) {
        this.status = status;
        return this;
    }

    /**
     * DB에 실제로 사용자를 생성하고 반환
     * ThreadLocal orgId를 자동으로 사용
     *
     * @param userRepository             사용자 Repository
     * @param membersRepository          멤버십 Repository
     * @param membershipRolesRepository  멤버십 역할 Repository
     * @param passwordEncoder            비밀번호 인코더
     * @return 생성된 사용자 DTO
     */
    public UserMemberDTO build(
            UserRepository userRepository,
            MembersRepository membersRepository,
            MembershipRolesRepository membershipRolesRepository,
            PasswordEncoder passwordEncoder
    ) {
        // 중복 확인
        UserMemberDTO existing = userRepository.fetchUserWithDetailsByEmail(email).orElse(null);
        if (existing != null) {
            return existing;
        }

        // 1. Users 생성 (DAO 패턴)
        Users user = new Users();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setIsDeleted(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setModifiedAt(LocalDateTime.now());

        Users savedUser = userRepository.insert(user);

        // 2. Memberships 생성 (DAO 패턴)
        Long orgId = TenantContext.getRequiredOrgId();
        Memberships membership = new Memberships();
        membership.setOrgId(orgId);
        membership.setUserId(savedUser.getId());
        membership.setName(name);
        membership.setStatus(status);
        membership.setCreatedAt(LocalDateTime.now());
        membership.setModifiedAt(LocalDateTime.now());

        Memberships savedMembership = membersRepository.insert(membership);

        // 3. Roles 할당
        List<Long> actualRoleIds = roleIds.isEmpty()
                ? List.of(TestDataManager.getTestRoleId())
                : roleIds;

        for (int i = 0; i < actualRoleIds.size(); i++) {
            membershipRolesRepository.assignRole(
                    savedMembership.getId(),
                    actualRoleIds.get(i),
                    i == 0,  // 첫 번째를 primary로 설정
                    savedUser.getId()
            );
        }

        // 4. 최종 DTO 반환
        return userRepository.fetchUserWithDetailsById(savedUser.getId())
                .orElseThrow(() -> new RuntimeException(
                        "Fixture: 사용자 생성 실패 - user_id=" + savedUser.getId() + ", org_id=" + orgId
                ));
    }
}
