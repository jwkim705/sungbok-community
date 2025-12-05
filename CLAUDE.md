# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot 4.0 community platform with JWT authentication, OAuth2 social login (Google, Kakao, Naver), and PostgreSQL database using jOOQ for type-safe SQL queries. The application uses ES256 (ECDSA) algorithm for JWT tokens and Valkey (Redis fork) for refresh token storage.

**Key Technologies:**
- Java 25 (Spring Boot 4.0.0)
- jOOQ 3.20.9 for database access
- PostgreSQL 18.1 database
- Valkey/Redis for session and token storage
- JWT with ES256 (ECDSA P-256) signature algorithm
- OAuth2 (Google, Kakao, Naver)
- Spring REST Docs + OpenAPI 3 (Swagger UI)
- Gradle build system with multi-module project

## Build Commands

### Local Development Setup

**Start infrastructure (PostgreSQL + Valkey):**
```bash
# Set required environment variables first
export POSTGRES_USER=admin
export POSTGRES_PASSWORD=1234
export POSTGRES_DB=community
export VALKEY_PASSWORD=1234

# Use docker-compose or podman-compose
podman compose up -d postgres valkey
```

**Build and run:**
```bash
./gradlew clean build    # Clean build with tests
./gradlew build          # Build without clean
./gradlew bootRun        # Run application (local profile)
```

**Generate jOOQ code:**
```bash
# Requires PostgreSQL running locally
./gradlew generateJooq
```

**Testing:**
```bash
./gradlew test                                    # Run all tests
./gradlew test --tests UserControllerTest         # Run specific test class
./gradlew test --tests UserControllerTest.signup  # Run specific test method
```

**API Documentation:**
```bash
./gradlew openapi3       # Generate OpenAPI 3 spec from REST Docs
# Access Swagger UI at: http://localhost:8080/api/swagger-ui.html
```

## Architecture

### Package Structure

```
com.sungbok.community/
├── controller/          # REST endpoints
├── service/
│   ├── get/            # Query services (read operations)
│   └── change/         # Command services (write operations)
├── repository/         # jOOQ repositories (data access)
├── dto/                # Data transfer objects
├── security/
│   ├── config/         # Security configuration
│   ├── jwt/            # JWT token provider, filters, properties
│   ├── handler/        # Auth success/failure handlers
│   ├── service/        # UserDetails implementations
│   ├── provider/       # Authentication providers
│   └── model/          # Security domain models (PrincipalDetails, OAuthAttributes)
├── config/             # Application configuration
├── common/
│   ├── dto/            # Common response DTOs
│   ├── exception/      # Custom exceptions and handlers
│   ├── vo/             # Value objects
│   └── constant/       # Constants
├── enums/              # Enumerations
└── util/               # Utility classes
```

### Authentication Flow

1. **Form Login & OAuth2:** Both generate JWT tokens via `CustomAuthenticationSuccessHandler`
2. **JWT Filter (`JwtAuthenticationFilter`):** Validates tokens on every request before Spring Security filters
3. **Stateless Sessions:** `SessionCreationPolicy.STATELESS` - no server-side sessions
4. **Token Storage:** Refresh tokens stored in Valkey with email as key
5. **Token Refresh:** `/auth/refresh` endpoint accepts refresh token and returns new access token

### jOOQ Code Generation

The project uses a custom generator strategy (`JPrefixGeneratorStrategy` from `jooq-custom` submodule) that prefixes generated classes with 'J'. Generated code is placed in `build/generated-src/jooq/main` and included in source sets.

**Important:** jOOQ code generation requires:
- PostgreSQL running locally on port 5432
- Database credentials: admin/1234
- Database name: community
- Environment variables can override defaults via `DB_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`

### Database Access Pattern

Use jOOQ DSLContext directly in repositories. Generated jOOQ classes are in the `org.jooq.generated` package with 'J' prefix:
- Tables: `JUsers`, `JMembers`, `JPosts`, etc.
- Records: `JUsersRecord`, `JMembersRecord`, etc.
- DAOs: `JUsersDao`, `JMembersDao`, etc.

### Repository Layer Standard

This project follows a **hybrid DAO + DSL approach** for jOOQ data access:
- **Simple CRUD**: Use generated DAO methods for straightforward operations
- **Complex queries**: Use DSLContext for joins, aggregations, dynamic conditions
- **Updates**: Pattern-based approach (see below)

**Core Principles:**

1. **Constructor Standard**: Always `(DSLContext dsl, Configuration configuration)` in that order
2. **Field Naming**: Always `dsl` and `dao` (not `dslContext`, `entityDao`, etc.)
3. **No Business Dependencies**: Never inject encoders, validators, or business services into repositories
4. **Return Types**: Use `Optional<>` for single entity queries, `List<>` for collections (never null)
5. **Soft Delete**: Standard pattern using `is_deleted` flag with DSLContext.update()
6. **Separation of Concerns**: Repositories handle data access only - no business logic, validation, or default value setting

**Standard Repository Template:**

```java
@Repository
public class EntityRepository {
    private final DSLContext dsl;
    private final EntityDao dao;

    public EntityRepository(DSLContext dsl, Configuration configuration) {
        this.dsl = dsl;
        this.dao = new EntityDao(configuration);
    }

    // Simple CRUD: Use DAO
    public Optional<Entity> findById(Long id) {
        return Optional.ofNullable(dao.findById(id));
    }

    // Insert with RETURNING: Use DSL
    public Entity insert(Entity entity) {
        return dsl.insertInto(ENTITY)
                .set(dsl.newRecord(ENTITY, entity))
                .returning()
                .fetchOneInto(Entity.class);
    }

    // Complex queries: Use DSL
    public List<Entity> findByDynamicCriteria(SearchCriteria criteria) {
        return dsl.selectFrom(ENTITY)
                .where(
                    eq(ENTITY.STATUS, criteria.getStatus()),
                    containsIfNotBlank(ENTITY.TITLE, criteria.getSearch())
                )
                .fetchInto(Entity.class);
    }
}
```

**Update Pattern Guide:**

```
Decision tree:
├─ Single/few fields? → DSLContext.update()
├─ Multiple records with same change? → UpdatableRecord + batchUpdate()
├─ Uncertain if record exists? → Record.store() (upsert)
```

**Pattern 1: DSLContext.update() (Primary)**

Use for single field or partial updates:

```java
public int updateStatus(Long id, String status) {
    return dsl.update(ENTITY)
            .set(ENTITY.STATUS, status)
            .set(ENTITY.MODIFIED_AT, LocalDateTime.now())
            .where(ENTITY.ID.eq(id))
            .execute();
}
```

**Pattern 2: Record.store() (Upsert)**

Use when record may or may not exist:

```java
public Entity upsert(Entity entity) {
    EntityRecord record = dsl.newRecord(ENTITY, entity);
    record.store(); // INSERT if new, UPDATE if exists
    return record.into(Entity.class);
}
```

**Pattern 3: UpdatableRecord + batchUpdate() (Bulk)**

Use for batch updates:

```java
public void bulkUpdateStatus(List<Long> ids, String status) {
    List<EntityRecord> records = dsl.fetch(ENTITY, ENTITY.ID.in(ids));
    records.forEach(record -> {
        record.setStatus(status);
        record.setModifiedAt(LocalDateTime.now());
        record.changed(ENTITY.CREATED_AT, false); // Exclude from update
    });
    dsl.batchUpdate(records).execute();
}
```

**Advanced Patterns:**

**MULTISET for Nested Collections** (12x faster than fetchGroups):

```java
public PostDTO findPostWithComments(Long postId) {
    return dsl.select(
            POSTS.TITLE,
            POSTS.CONTENT,
            multiset(
                dsl.select(COMMENTS.CONTENT, COMMENTS.USER_ID)
                    .from(COMMENTS)
                    .where(COMMENTS.POST_ID.eq(POSTS.POST_ID))
                    .and(COMMENTS.IS_DELETED.eq(false))
            ).as("comments")
        )
        .from(POSTS)
        .where(POSTS.POST_ID.eq(postId))
        .fetchOneInto(PostDTO.class);
}
```

**Dynamic Queries with Condition Utilities:**

```java
import static com.sungbok.community.repository.util.JooqConditionUtils.eq;
import static com.sungbok.community.repository.util.JooqStringConditionUtils.*;

public List<Post> findByDynamicCriteria(SearchCriteria criteria) {
    return dsl.selectFrom(POSTS)
        .where(
            eq(POSTS.STATUS, criteria.getStatus()),
            containsIfNotBlank(POSTS.TITLE, criteria.getSearch()),
            inIfNotEmpty(POSTS.CATEGORY, criteria.getCategories())
        )
        .fetchInto(Post.class);
}
```

**GROUP_CONCAT for Aggregation:**

```java
public List<UserWithRolesDTO> findUsersWithRoles() {
    return dsl.select(
            USERS.ID,
            USERS.EMAIL,
            DSL.groupConcat(ROLES.NAME).separator(", ").as("roleNames")
        )
        .from(USERS)
        .leftJoin(USER_ROLES).on(USER_ROLES.USER_ID.eq(USERS.ID))
        .leftJoin(ROLES).on(ROLES.ID.eq(USER_ROLES.ROLE_ID))
        .groupBy(USERS.ID, USERS.EMAIL)
        .fetchInto(UserWithRolesDTO.class);
}
```

**DO Examples:**

```java
// ✓ Simple query - use DAO
public Optional<User> findById(Long id) {
    return Optional.ofNullable(dao.findById(id));
}

// ✓ Partial update - use DSLContext
public int incrementViewCount(Long postId) {
    return dsl.update(POSTS)
            .set(POSTS.VIEW_COUNT, POSTS.VIEW_COUNT.add(1))
            .where(POSTS.POST_ID.eq(postId))
            .execute();
}

// ✓ Soft delete standard
public int softDelete(Long id) {
    return dsl.update(ENTITY)
            .set(ENTITY.IS_DELETED, true)
            .set(ENTITY.MODIFIED_AT, LocalDateTime.now())
            .where(ENTITY.ID.eq(id))
            .execute();
}
```

**DON'T Examples:**

```java
// ✗ Business logic in repository
public User save(UserDTO dto) {
    User user = new User()
        .setPassword(passwordEncoder.encode(dto.getPassword())) // NO!
        .setCreatedAt(LocalDateTime.now()); // Let service set or use DB default
    return dao.insert(user);
}

// ✗ Wrong constructor order
public UserRepository(Configuration configuration, DSLContext dsl) // NO!

// ✗ Inconsistent field naming
private final DSLContext dslContext; // Use 'dsl'
private final UsersDao usersDao; // Use 'dao'

// ✗ Using DAO.update() for partial updates
public void updateStatus(User user) {
    dao.update(user); // Updates ALL fields unnecessarily
}
```

**Reference:** For detailed jOOQ patterns and performance analysis, see [jooq-semina project](https://github.com/cheese10yun/jooq-semina).

### jOOQ Method Naming Conventions

**This project uses jOOQ conventions, NOT JPA conventions.**

**Query Methods (조회):**
- `fetch*`: 단일/복수 결과 조회
  ```java
  fetchById(Long id)         // ID로 조회
  fetchByEmail(String email) // 이메일로 조회
  fetchAll()                 // 전체 조회
  fetchAllPosts(SearchVO)    // 복수 조회
  fetchUserWithDetailsById() // JOIN 조회
  ```

- `exists*`: 존재 여부 확인
  ```java
  existsById(Long id)
  existsByEmail(String email)
  ```

- `count*`: 개수 조회
  ```java
  countAll()
  countByStatus(String status)
  ```

**Mutation Methods (변경):**
- `insert`: 새 레코드 삽입 (항상 RETURNING 절 사용)
- `update`: 레코드 수정 (DSLContext.update() 패턴)
- `delete`: 레코드 삭제
  - `softDelete(Long id)` - Soft delete (권장)
  - `hardDelete(Long id)` - Hard delete (주의)

**Enum Methods:**
- `fromCode()` - 코드로 Enum 조회

**DO NOT Use (JPA 스타일):**
- `find*` - JPA 스타일, 사용하지 마세요
- `save*` - JPA 스타일, insert/update로 명확히 구분
- `remove*` - JPA 스타일, delete 사용
- `findByCode()` - Enum에서 사용하지 마세요, fromCode() 사용

**Examples:**
```java
// ✓ GOOD - jOOQ 스타일
public Optional<User> fetchById(Long id) { ... }
public User insert(User user) { ... }
public int update(User user) { ... }
public int softDelete(Long id) { ... }
public static SocialType fromCode(String code) { ... }

// ✗ BAD - JPA 스타일
public Optional<User> findById(Long id) { ... }
public User save(User user) { ... }
public static SocialType findByCode(String code) { ... }
```

### Code Documentation Guidelines

**All JavaDoc comments must be written in simple, concise Korean.**

**Class Level:**
```java
/**
 * 사용자 데이터 접근 Repository
 * 하이브리드 DAO + DSL 패턴 사용
 */
```

**Method Level:**
```java
/**
 * ID로 사용자를 조회합니다.
 *
 * @param id 사용자 ID
 * @return 사용자 Optional (없으면 빈 Optional)
 */
public Optional<User> fetchById(Long id) { ... }

/**
 * RETURNING 절로 새 사용자를 삽입합니다.
 * 비즈니스 로직은 Service 레이어에서 처리
 *
 * @param user 삽입할 사용자 엔티티
 * @return 생성된 ID와 타임스탬프가 포함된 삽입된 사용자
 */
public User insert(User user) { ... }
```

**Technical Terms:**
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

**Enum Method Documentation:**
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

### jOOQ vs JPA - Critical Differences

**This project uses jOOQ, NOT JPA!**

**DO NOT (절대 하지 마세요):**
```java
// ✗ JPA Entity 직접 생성 금지
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue
    private Long id;

    @OneToMany(mappedBy = "user")  // 관계 매핑 금지!
    private List<Member> members;

    @ManyToOne
    @JoinColumn(name = "dept_id")  // 관계 매핑 금지!
    private Department department;
}

// ✗ Repository에서 관계 자동 로딩 기대
user.getMembers(); // LazyInitializationException 발생하지 않음 - 그냥 null/empty
```

**DO (올바른 jOOQ 방식):**
```java
// ✓ jOOQ가 생성한 POJO 사용
import org.jooq.generated.tables.pojos.Users;
import org.jooq.generated.tables.pojos.Members;

// ✓ 관계는 명시적 JOIN으로 표현
public List<UserMemberDTO> fetchUsersWithMembers() {
    return dsl.select(
            USERS.fields(),
            MEMBERS.fields()
        )
        .from(USERS)
        .join(MEMBERS).on(MEMBERS.USER_ID.eq(USERS.ID))  // 명시적 JOIN
        .fetchInto(UserMemberDTO.class);
}

// ✓ MULTISET으로 중첩 컬렉션 표현
public UserWithMembersDTO fetchUserWithMembers(Long userId) {
    return dsl.select(
            USERS.fields(),
            multiset(
                dsl.select(MEMBERS.fields())
                    .from(MEMBERS)
                    .where(MEMBERS.USER_ID.eq(USERS.ID))
            ).as("members")
        )
        .from(USERS)
        .where(USERS.ID.eq(userId))
        .fetchOneInto(UserWithMembersDTO.class);
}
```

**Key Principles:**
1. **Entity 클래스 직접 생성 금지**: jOOQ가 자동 생성한 POJO 사용
2. **관계 매핑 어노테이션 금지**: @OneToMany, @ManyToOne, @ManyToMany 등 사용 불가
3. **FK는 DB에서 관리**: 관계는 데이터베이스 스키마에서 정의
4. **명시적 JOIN 사용**: 코드에서 관계를 표현할 때는 JOIN 또는 MULTISET 사용
5. **Generated 코드 수정 금지**: `build/generated-src/jooq/main` 폴더의 코드는 건드리지 않음

**Why jOOQ over JPA?**
- **Type Safety**: Compile-time query validation
- **Performance**: No hidden N+1 queries, explicit control
- **Transparency**: See exactly what SQL is generated
- **Flexibility**: Full SQL feature support (MULTISET, window functions, CTEs)
- **No Magic**: No lazy loading, no proxy objects, no automatic dirty checking

### Service Layer Pattern

Services are organized by operation type:
- **Get Services:** Read-only operations, queries
- **Change Services:** Write operations (create, update, delete)

### Security Configuration

**Public Endpoints:**
- `/health-check`
- `/users/signup`
- `GET /posts`, `GET /posts/**`
- `/auth/**` (login, token refresh)
- `/oauth2/**`, `/login/oauth2/code/**`
- API docs: `/v3/api-docs/**`, `/swagger-ui/**`

**JWT Configuration:**
- Algorithm: ES256 (ECDSA P-256)
- Private key: `src/main/resources/keys/jwt-private.pem`
- Public key: `src/main/resources/keys/jwt-public.pem`
- Access token: 15 minutes
- Refresh token: 7 days
- Header: `Authorization: Bearer <token>`

**Generating JWT Keys (if needed):**
```bash
# Generate EC private key (P-256 curve)
openssl ecparam -name prime256v1 -genkey -noout -out jwt-private-ec.pem

# Convert to PKCS8 format
openssl pkcs8 -topk8 -nocrypt -in jwt-private-ec.pem -out jwt-private.pem

# Extract public key
openssl ec -in jwt-private-ec.pem -pubout -out jwt-public.pem
```

### Multi-Tenancy Architecture

**This application uses a SaaS multi-tenancy pattern with shared database and shared schema.**

#### Tenant Isolation Strategy

- **Pattern:** Shared Database, Shared Schema with `app_id` discriminator
- **Tenant Identifier:** `app_id` (BIGINT, references `apps.app_id`)
- **Isolation Level:** Row-level security via application logic (ThreadLocal + jOOQ)
- **JWT Integration:** JWT access tokens include `appId` claim for tenant identification

#### Tables with app_id (Tenant-Scoped)

13 tables include `app_id` for tenant isolation:
```
users, oauth_accounts, members, posts, comments, post_likes,
post_youtube, files, attendance, family_relations, groups, membership
```

**Shared Reference Tables (NO app_id):**
- `departments` - Department hierarchy (shared across all tenants)
- `roles` - User role definitions (shared across all tenants)
- `posts_categories` - Post category definitions (shared across all tenants)

#### JWT app_id Claim Standard

Access tokens include `appId` claim for tenant identification:
```json
{
  "sub": "user@example.com",
  "appId": 1,
  "userId": 123,
  "name": "홍길동",
  "role": "MEMBER",
  "iss": "sungbok-community",
  "iat": 1234567890,
  "exp": 1234568790
}
```

Refresh tokens contain only minimal claims (email, issuer) for security.

#### Repository Layer Filtering Pattern

All repositories MUST use `TenantContext` for automatic app_id filtering:

```java
import static com.sungbok.community.repository.util.JooqTenantConditionUtils.appIdCondition;

public Optional<Users> fetchByEmail(String email) {
    return dsl.selectFrom(USERS)
            .where(appIdCondition(USERS.APP_ID))  // Automatic tenant filter
            .and(USERS.EMAIL.eq(email))
            .and(USERS.IS_DELETED.eq(false))
            .fetchOptionalInto(Users.class);
}
```

**Key Rules:**

1. **Always use `appIdCondition(TABLE.APP_ID)` in WHERE clauses** for tenant-scoped tables
2. **Always join on app_id first** when joining tenant-scoped tables:
   ```java
   .join(MEMBERS).on(
       MEMBERS.APP_ID.eq(USERS.APP_ID)  // Join on app_id first
               .and(MEMBERS.USER_ID.eq(USERS.ID))
   )
   ```
3. **Include APP_ID in SELECT** when populating DTOs that will be returned to clients
4. **Never trust user input for app_id** - always use `TenantContext.getRequiredAppId()`
5. **Shared reference tables don't need app_id filtering** (departments, roles, posts_categories)
6. **MULTISET queries must filter by app_id**:
   ```java
   multiset(
       dsl.select(FILES.fields())
           .from(FILES)
           .where(FILES.APP_ID.eq(POSTS.APP_ID))  // Tenant filter in MULTISET
           .and(FILES.RELATED_ENTITY_ID.eq(POSTS.POST_ID))
   ).as("files")
   ```

**INSERT Pattern:**
```java
public Users insert(Users user) {
    // TenantContext에서 app_id 가져오기
    Long appId = TenantContext.getRequiredAppId();
    user.setAppId(appId);  // 강제로 현재 테넌트 설정

    UsersRecord record = dsl.newRecord(USERS, user);
    return dsl.insertInto(USERS)
            .set(record)
            .returning()
            .fetchOneInto(Users.class);
}
```

**UPDATE Pattern:**
```java
public int update(Users user) {
    return dsl.update(USERS)
            .set(USERS.EMAIL, user.getEmail())
            .set(USERS.MODIFIED_AT, LocalDateTime.now())
            .where(appIdCondition(USERS.APP_ID))  // Tenant filter
            .and(USERS.ID.eq(user.getId()))
            .execute();
}
```

**SOFT DELETE Pattern:**
```java
public int softDelete(Long userId) {
    return dsl.update(USERS)
            .set(USERS.IS_DELETED, true)
            .set(USERS.MODIFIED_AT, LocalDateTime.now())
            .where(appIdCondition(USERS.APP_ID))  // Tenant filter
            .and(USERS.ID.eq(userId))
            .execute();
}
```

#### TenantContext (ThreadLocal)

`TenantContext` stores the current request's `app_id` using ThreadLocal:

```java
// Automatically set by JwtAuthenticationFilter
Long appId = TenantContext.getRequiredAppId();  // Throws if not set

// Cleanup is handled automatically in filter's finally block
```

**Important:** Never manually set `TenantContext` in application code. It's automatically managed by `JwtAuthenticationFilter`.

#### Security Considerations

1. **Every tenant-scoped query MUST filter by app_id** - no exceptions
2. **No cross-tenant queries allowed** - tenant isolation is enforced at application level
3. **JWT validation verifies appId matches database** - prevents token tampering
4. **ThreadLocal cleanup prevents memory leaks** - always cleared in filter's finally block
5. **INSERT operations force app_id** - prevents malicious clients from specifying wrong tenant
6. **Composite foreign keys enforce referential integrity** - related records must belong to same tenant

#### Testing Multi-Tenancy

When writing tests that involve tenant-scoped data:

1. **Set up TenantContext in test fixtures:**
   ```java
   @BeforeEach
   void setUp() {
       TenantContext.setAppId(1L);  // Set test tenant
   }

   @AfterEach
   void tearDown() {
       TenantContext.clear();  // Cleanup
   }
   ```

2. **Test cross-tenant isolation** - verify queries don't return data from other tenants
3. **Test JWT token generation** - ensure appId claim is included
4. **Test filter behavior** - ensure TenantContext is properly set and cleaned up

### CORS Configuration

Allowed origins configured in `application.yml`:
- `http://localhost:3000` (React/Next.js)
- `http://localhost:19006` (Expo)

### Error Handling

Global exception handling via `@RestControllerAdvice` in `ErrorHandlerAdvice`. Custom exceptions:
- `AlreadyExistException`
- `BadRequestException`
- `DataNotFoundException`
- `NotFoundException`
- `NotMatchPwdException`
- `UnAuthorizedException`
- `SystemException`

## Testing

Tests use:
- `@SpringBootTest` with `@AutoConfigureMockMvc` for integration tests
- `@ActiveProfiles("test")` to use `application-test.yml`
- `@MockitoSpyBean` for partial mocking
- Spring REST Docs with `restdocs-api-spec` for API documentation generation

Test profile uses hardcoded credentials (test data) rather than environment variables.

## Environment Variables

**Required for local development:**
```bash
POSTGRES_USER=admin
POSTGRES_PASSWORD=1234
POSTGRES_DB=community
VALKEY_PASSWORD=1234
```

**Required for OAuth2 (optional for basic development):**
```bash
GOOGLE_CLIENT_ID=<your-google-client-id>
GOOGLE_CLIENT_SECRET=<your-google-client-secret>
KAKAO_CLIENT_ID=<your-kakao-client-id>
KAKAO_CLIENT_SECRET=<your-kakao-client-secret>
NAVER_CLIENT_ID=<your-naver-client-id>
NAVER_CLIENT_SECRET=<your-naver-client-secret>
```

**Optional for deployment:**
```bash
DB_URL=jdbc:postgresql://host:5432/dbname
PGADMIN_DEFAULT_EMAIL=<email>
PGADMIN_DEFAULT_PASSWORD=<password>
```

## Multi-Module Structure

The project includes a `jooq-custom` submodule for custom jOOQ code generation strategies. When modifying jOOQ generation behavior, update the submodule at `jooq-custom/`.

## Deployment

The project supports blue-green deployment with separate Docker Compose files in the `docker/` directory. The application runs on context path `/api` (configured in `application.yml`).

## Important Notes

- **No Session Management:** Application is fully stateless, using JWT tokens
- **Java 25:** Uses latest Java features and Spring Boot 4.0
- **jOOQ First:** This project uses jOOQ, not JPA/Hibernate
- **Log4j2:** Using Log4j2 instead of Logback (Spring Boot default is excluded)
- **XSS Protection:** Custom `HtmlCharacterEscapes` for JSON serialization
- **Production URLs:** API base URL: `https://sungbok.p-e.kr/api`