# Code Documentation Guidelines

**All JavaDoc comments must be written in simple, concise Korean.**

## Class Level Documentation

```java
/**
 * 사용자 데이터 접근 Repository
 * 하이브리드 DAO + DSL 패턴 사용
 */
@Repository
public class UsersRepository {
    // ...
}
```

## Method Level Documentation

### Query Methods

```java
/**
 * ID로 사용자를 조회합니다.
 *
 * @param id 사용자 ID
 * @return 사용자 Optional (없으면 빈 Optional)
 */
public Optional<Users> fetchById(Long id) {
    return Optional.ofNullable(dao.findById(id));
}
```

### Mutation Methods

```java
/**
 * RETURNING 절로 새 사용자를 삽입합니다.
 * 비즈니스 로직은 Service 레이어에서 처리
 *
 * @param user 삽입할 사용자 엔티티
 * @return 생성된 ID와 타임스탬프가 포함된 삽입된 사용자
 */
public Users insert(Users user) {
    return dsl.insertInto(USERS)
            .set(dsl.newRecord(USERS, user))
            .returning()
            .fetchOneInto(Users.class);
}
```

## Technical Terms

Technical terms should be kept in English with Korean explanation if needed:

```java
// ✓ GOOD
// MULTISET으로 중첩 컬렉션 조회 (fetchGroups 대비 12배 빠름)
multiset(...)

// RETURNING 절로 생성된 값 반환
.returning()

// DSLContext.update() 패턴 사용
dsl.update(...)
```

## Enum Method Documentation

```java
/**
 * 코드 문자열로 SocialType을 찾습니다.
 *
 * @param code 소셜 타입 코드
 * @return 매칭되는 SocialType
 * @throws DataNotFoundException 매칭되는 값이 없을 때
 */
public static SocialType fromCode(String code) {
    return Arrays.stream(values())
            .filter(v -> v.code.equalsIgnoreCase(code))
            .findFirst()
            .orElseThrow(() -> new DataNotFoundException(...));
}
```

## Service Layer Documentation

```java
/**
 * 사용자를 조회하는 Query Service
 * Read-only 작업만 수행
 */
@Service
@Transactional(readOnly = true)
public class UserGetService {

    /**
     * 이메일로 사용자를 조회합니다.
     *
     * @param email 사용자 이메일
     * @return 사용자 정보
     * @throws DataNotFoundException 사용자가 없을 때
     */
    public UserDTO fetchByEmail(String email) {
        // ...
    }
}
```

## Controller Documentation

```java
/**
 * 사용자 관련 REST API
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    /**
     * 사용자 회원가입
     *
     * @param request 회원가입 요청 정보
     * @return 생성된 사용자 정보
     */
    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(@RequestBody SignupRequest request) {
        // ...
    }
}
```

## Documentation Best Practices

1. **간결하게**: 불필요한 설명 지양, 핵심만 전달
2. **Korean first**: 모든 설명은 한국어로 작성
3. **Technical terms in English**: 기술 용어는 영어 유지
4. **@param, @return, @throws**: 항상 명시
5. **Purpose over implementation**: 무엇을 하는지 설명, 어떻게 하는지는 최소화

## Examples

### ✓ GOOD

```java
/**
 * 사용자 상태를 업데이트합니다.
 *
 * @param userId 사용자 ID
 * @param status 새로운 상태
 * @return 영향받은 행 수
 */
public int updateStatus(Long userId, String status) { ... }
```

### ✗ BAD

```java
/**
 * This method updates the user's status field in the database
 * by executing a SQL UPDATE statement using jOOQ DSLContext.
 * It uses the DSLContext.update() pattern for partial updates.
 *
 * @param userId the ID of the user to update
 * @param status the new status value
 * @return the number of affected rows
 */
public int updateStatus(Long userId, String status) { ... }
```

The bad example is:
- Too verbose (설명이 너무 장황함)
- In English (한국어가 아님)
- Explains implementation details (구현 세부사항 설명)
