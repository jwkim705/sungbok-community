# Testing Guidelines

테스트 코드 작성 시 Repository 패턴을 사용하여 데이터 접근 계층의 일관성을 유지하는 가이드라인입니다.

## 핵심 규칙

### ✅ DO: Repository 메서드 사용

테스트 코드에서 데이터베이스 작업 시 **반드시 Repository 메서드를 사용**합니다.

```java
@SpringBootTest
class MyTest {

    @Autowired
    UserRepository userRepository;

    @Autowired
    MembersRepository membersRepository;

    @Autowired
    MembershipRolesRepository membershipRolesRepository;

    @Test
    void testExample() {
        // ✅ GOOD: Repository 메서드 사용
        Users user = new Users();
        user.setEmail("test@example.com");
        user.setPassword("password");

        Users savedUser = userRepository.insert(user);

        // ✅ GOOD: Repository 메서드로 role 할당
        membershipRolesRepository.assignRole(
            membershipId,
            roleId,
            true,  // isPrimary
            assignedBy
        );
    }
}
```

### ❌ DON'T: DSL 직접 사용 금지

테스트 코드에서 `DSLContext`를 직접 사용하여 `insertInto()`, `update()`, `delete()` 등을 호출하지 **않습니다**.

```java
// ❌ BAD: DSL 직접 사용
@Autowired
DSLContext dsl;

@Test
void testBadExample() {
    // ❌ BAD: dsl.insertInto() 직접 사용
    Users savedUser = dsl.insertInto(USERS)
            .set(dsl.newRecord(USERS, user))
            .returning()
            .fetchOneInto(Users.class);

    // ❌ BAD: dsl.insertInto()로 role 할당
    dsl.insertInto(MEMBERSHIP_ROLES)
            .set(MEMBERSHIP_ROLES.ORG_ID, orgId)
            .set(MEMBERSHIP_ROLES.MEMBERSHIP_ID, membershipId)
            .set(MEMBERSHIP_ROLES.ROLE_ID, roleId)
            .set(MEMBERSHIP_ROLES.IS_PRIMARY, true)
            .set(MEMBERSHIP_ROLES.ASSIGNED_BY, userId)
            .execute();
}
```

## 왜 Repository 패턴을 사용해야 하나요?

### 1. 코드 일관성
- 테스트 코드와 프로덕션 코드 모두 Repository 계층을 통해 데이터 접근
- 동일한 패턴으로 작성된 코드는 이해하기 쉽고 유지보수가 용이

### 2. 멀티테넌시 자동 관리
- Repository 메서드는 `TenantContext`에서 자동으로 `orgId` 가져옴
- DSL 직접 사용 시 `orgId`를 수동으로 설정해야 하며, 누락 시 버그 발생

**예시**:
```java
// ✅ GOOD: orgId 자동 설정
Memberships savedMembership = membersRepository.insert(membership);
// → MembersRepository.insert()에서 TenantContext.getRequiredOrgId() 자동 호출

// ❌ BAD: orgId 수동 설정 (누락 위험)
dsl.insertInto(MEMBERSHIPS)
        .set(MEMBERSHIPS.ORG_ID, testDataHelper.getTestOrgId())  // 수동 설정
        .set(dsl.newRecord(MEMBERSHIPS, membership))
        .execute();
```

### 3. 코드 간결화
- Repository 메서드는 복잡한 jOOQ 코드를 캡슐화
- 4~7줄의 DSL 코드가 1~5줄의 간결한 메서드 호출로 변경

**비교**:
```java
// ❌ BAD: 4줄
Users savedUser = dsl.insertInto(USERS)
        .set(dsl.newRecord(USERS, user))
        .returning()
        .fetchOneInto(Users.class);

// ✅ GOOD: 1줄
Users savedUser = userRepository.insert(user);
```

### 4. 유지보수성
- Repository 로직 변경 시 한 곳만 수정
- 테스트 코드에서 DSL 직접 사용 시 여러 곳을 수정해야 함

### 5. 트랜잭션 안전성
- Repository의 DAO 패턴은 트랜잭션을 안전하게 관리
- 자동 ID 생성, 반환값 보장 등

## 주입받아야 할 Repository 목록

테스트 코드에서 데이터베이스 작업 시 다음 Repository를 `@Autowired`로 주입받아 사용합니다:

| Repository | 용도 | 주요 메서드 |
|-----------|------|-----------|
| `UserRepository` | Users 테이블 작업 | `insert()`, `fetchById()`, `fetchByEmail()` |
| `MembersRepository` | Memberships 테이블 작업 | `insert()`, `fetchById()`, `fetchByUserId()` |
| `MembershipRolesRepository` | MembershipRoles 테이블 작업 | `assignRole()`, `fetchByMembershipId()` |
| `RolesRepository` | Roles 테이블 작업 | `insert()`, `fetchById()` |
| `OrganizationsRepository` | Organizations 테이블 작업 | `insert()`, `fetchById()` |
| 기타... | 필요한 Repository 추가 | - |

## 코드 예시

### 예시 1: 사용자 생성 테스트

```java
@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    UserRepository userRepository;

    @Autowired
    MembersRepository membersRepository;

    @Autowired
    MembershipRolesRepository membershipRolesRepository;

    @Autowired
    TestDataHelper testDataHelper;

    @Test
    void createUser_Success() {
        // Given: 사용자 생성
        Users user = new Users();
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setIsDeleted(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setModifiedAt(LocalDateTime.now());

        Users savedUser = userRepository.insert(user);  // ✅ Repository 사용

        // Given: Membership 생성
        Memberships membership = new Memberships();
        membership.setUserId(savedUser.getId());
        membership.setName("테스트 사용자");
        membership.setStatus("APPROVED");
        membership.setCreatedAt(LocalDateTime.now());
        membership.setModifiedAt(LocalDateTime.now());

        Memberships savedMembership = membersRepository.insert(membership);  // ✅ Repository 사용

        // Given: Role 할당
        membershipRolesRepository.assignRole(  // ✅ Repository 사용
                savedMembership.getId(),
                testDataHelper.getTestRoleId(),
                true,  // isPrimary
                savedUser.getId()
        );

        // When & Then: 검증
        assertNotNull(savedUser.getId());
        assertNotNull(savedMembership.getId());
    }
}
```

### 예시 2: BaseAuthenticationTest 패턴

```java
public abstract class BaseAuthenticationTest {

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected MembersRepository membersRepository;

    @Autowired
    protected MembershipRolesRepository membershipRolesRepository;

    protected UserMemberDTO createTestUser(String email, String password, List<Long> roleIds) {
        // ✅ GOOD: Repository 사용
        Users user = new Users();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setIsDeleted(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setModifiedAt(LocalDateTime.now());

        Users savedUser = userRepository.insert(user);

        Memberships membership = new Memberships();
        membership.setUserId(savedUser.getId());
        membership.setName("테스트 사용자");
        membership.setStatus("APPROVED");
        membership.setCreatedAt(LocalDateTime.now());
        membership.setModifiedAt(LocalDateTime.now());

        Memberships savedMembership = membersRepository.insert(membership);

        // Role 할당
        for (int i = 0; i < roleIds.size(); i++) {
            membershipRolesRepository.assignRole(
                    savedMembership.getId(),
                    roleIds.get(i),
                    i == 0,  // 첫 번째 역할을 primary로 설정
                    savedUser.getId()
            );
        }

        return userRepository.fetchUserWithDetailsById(savedUser.getId())
                .orElseThrow(() -> new RuntimeException("Test user not found"));
    }
}
```

## 예외 사항

다음의 경우에만 DSL 직접 사용이 허용됩니다:

### 1. Repository 자체를 테스트하는 경우

```java
@SpringBootTest
class UserRepositoryTest {

    @Autowired
    DSLContext dsl;  // Repository 테스트에서만 허용

    @Test
    void testRepositoryDirectly() {
        // Repository의 insert() 메서드를 테스트하기 위해 DSL 직접 사용
        var result = dsl.selectFrom(USERS)
                .where(USERS.EMAIL.eq("test@example.com"))
                .fetchOne();

        assertNotNull(result);
    }
}
```

### 2. 복잡한 동적 쿼리를 테스트하는 경우

Repository에 없는 특수한 쿼리 로직을 테스트해야 하는 경우에만 DSL을 직접 사용할 수 있습니다. 이 경우에도 최소한으로 사용하고, 가능하면 Repository에 메서드를 추가하는 것을 권장합니다.

## 요약

| 구분 | 내용 |
|------|------|
| ✅ **DO** | Repository 메서드 사용 (`@Autowired`로 주입) |
| ❌ **DON'T** | DSL 직접 사용 (`dsl.insertInto()` 금지) |
| **장점** | 코드 일관성, 멀티테넌시 자동화, 간결화, 유지보수성, 트랜잭션 안전성 |
| **예외** | Repository 자체 테스트, 복잡한 동적 쿼리 테스트 (최소한으로) |

---

**참고 문서**:
- `agent_docs/jooq-patterns.md` - jOOQ Repository 패턴 상세 가이드
- `agent_docs/multi-tenancy.md` - Multi-tenancy 아키텍처 가이드
