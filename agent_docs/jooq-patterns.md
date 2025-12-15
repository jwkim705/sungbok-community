# jOOQ Repository Patterns

This document provides detailed guidelines for implementing repositories using jOOQ in the sungbok-community project.

## Hybrid DAO + DSL Approach

This project follows a **hybrid DAO + DSL approach** for jOOQ data access:
- **Simple CRUD**: Use generated DAO methods for straightforward operations
- **Complex queries**: Use DSLContext for joins, aggregations, dynamic conditions
- **Updates**: Pattern-based approach (see below)

## Core Principles

1. **Constructor Standard**: Always `(DSLContext dslContext, Configuration configuration)` in that order
2. **Field Naming**: Use `dslContext` for DSLContext and `dao` for DAOs (clear and explicit)
3. **DSL Usage**: Use `DSL.multiset()` explicitly (not static import) to avoid confusion with dslContext
4. **No Business Dependencies**: Never inject encoders, validators, or business services into repositories
5. **Return Types**: Use `Optional<>` for single entity queries, `List<>` for collections (never null)
6. **Soft Delete**: Standard pattern using `is_deleted` flag with DSLContext.update()
7. **Separation of Concerns**: Repositories handle data access only - no business logic, validation, or default value setting

## Insert Pattern Guide

jOOQ에서 INSERT는 **반환 타입**에 따라 패턴을 선택합니다.

### Pattern 1: DAO 사용 (POJO 반환) ✅ 권장

POJO 객체를 반환해야 할 때 사용합니다.

```java
public Posts insert(Posts post) {
    Long orgId = TenantContext.getRequiredOrgId();
    post.setOrgId(orgId);  // 동적 값 설정
    dao.insert(post);      // DAO로 삽입
    return post;           // PK와 모든 필드가 자동으로 채워짐
}
```

**장점:**
- 간단하고 명확
- PK가 자동으로 POJO에 설정됨
- 타임스탬프 등 DB default 값도 자동 반영

**언제 사용:**
- Repository의 insert() 메서드가 POJO를 반환할 때
- 삽입 후 전체 객체 정보가 필요할 때

### Pattern 2: values() + returningResult() (PK만 반환)

PK만 반환하면 되는 경우 (성능 최적화).

```java
public Long insert(Posts post) {
    Long orgId = TenantContext.getRequiredOrgId();

    return dslContext.insertInto(POSTS,
                    POSTS.ORG_ID,
                    POSTS.TITLE,
                    POSTS.CONTENT
                )
                .values(
                        orgId,
                        post.getTitle(),
                        post.getContent()
                )
                .returningResult(POSTS.POST_ID)
                .fetchOneInto(Long.class);
}
```

**장점:**
- PK만 가져오므로 성능 최적화
- 명시적인 필드 지정

**언제 사용:**
- 삽입 후 PK만 필요한 경우
- 추가 조회 없이 ID만으로 충분한 경우

### Pattern 3: values() + returning(fields()) (DTO 변환)

DTO로 변환해야 하는 경우.

```java
public PostDTO insert(Posts post) {
    Long orgId = TenantContext.getRequiredOrgId();

    return dslContext.insertInto(POSTS,
                    POSTS.ORG_ID,
                    POSTS.TITLE,
                    POSTS.CONTENT
                )
                .values(
                        orgId,
                        post.getTitle(),
                        post.getContent()
                )
                .returning(POSTS.fields())
                .fetchOneInto(PostDTO.class);
}
```

**장점:**
- POJO가 아닌 DTO로 직접 변환
- 필요한 필드만 선택 가능

**언제 사용:**
- POJO가 아닌 DTO를 반환할 때
- 특정 필드만 반환하고 싶을 때

### ❌ 안티패턴 (사용하지 마세요)

#### 1. newRecord() + insertInto().set()
```java
// ✗ BAD
return dsl.insertInto(POSTS)
        .set(dsl.newRecord(POSTS, post))  // 불필요한 변환
        .returning()
        .fetchOneInto(Posts.class);
```

#### 2. Manual POJO creation + newRecord()
```java
// ✗ BAD
MembershipRoles record = new MembershipRoles();
record.setOrgId(orgId);
record.setMembershipId(membershipId);
return dsl.insertInto(MEMBERSHIP_ROLES)
        .set(dsl.newRecord(MEMBERSHIP_ROLES, record))
        .returning()
        .fetchOneInto(MembershipRoles.class);
```

#### 3. Record + set()
```java
// ✗ BAD
UsersRecord record = dslContext.newRecord(USERS, user);
return dslContext.insertInto(USERS)
          .set(record)
          .returning()
          .fetchOneInto(Users.class);
```

**왜 안티패턴인가?**
- POJO를 반환한다면 DAO 패턴이 훨씬 간단하고 명확
- 불필요한 중간 변환 단계 (POJO → Record → INSERT)
- 코드가 복잡하고 읽기 어려움

## Standard Repository Template

```java
@Repository
public class EntityRepository {
    private final DSLContext dslContext;
    private final EntityDao dao;

    public EntityRepository(DSLContext dslContext, Configuration configuration) {
        this.dslContext = dslContext;
        this.dao = new EntityDao(configuration);
    }

    // Simple CRUD: Use DAO
    public Optional<Entity> fetchById(Long id) {
        return Optional.ofNullable(dao.findById(id));
    }

    // Insert with RETURNING: Use DSL
    public Entity insert(Entity entity) {
        return dslContext.insertInto(ENTITY)
                .set(dslContext.newRecord(ENTITY, entity))
                .returning()
                .fetchOneInto(Entity.class);
    }

    // Complex queries: Use DSL
    public List<Entity> fetchByDynamicCriteria(SearchCriteria criteria) {
        return dslContext.selectFrom(ENTITY)
                .where(
                    eq(ENTITY.STATUS, criteria.getStatus()),
                    containsIfNotBlank(ENTITY.TITLE, criteria.getSearch())
                )
                .fetchInto(Entity.class);
    }
}
```

## Update Pattern Guide

```
Decision tree:
├─ Single/few fields? → DSLContext.update()
├─ Multiple records with same change? → UpdatableRecord + batchUpdate()
├─ Uncertain if record exists? → Record.store() (upsert)
```

### Pattern 1: DSLContext.update() (Primary)

Use for single field or partial updates:

```java
public int updateStatus(Long id, String status) {
    return dslContext.update(ENTITY)
            .set(ENTITY.STATUS, status)
            .set(ENTITY.MODIFIED_AT, LocalDateTime.now())
            .where(ENTITY.ID.eq(id))
            .execute();
}
```

### Pattern 2: Record.store() (Upsert)

Use when record may or may not exist:

```java
public Entity upsert(Entity entity) {
    EntityRecord record = dsl.newRecord(ENTITY, entity);
    record.store(); // INSERT if new, UPDATE if exists
    return record.into(Entity.class);
}
```

### Pattern 3: UpdatableRecord + batchUpdate() (Bulk)

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

## Advanced Patterns

### MULTISET for Nested Collections (12x faster than fetchGroups)

**Basic MULTISET Pattern (nested objects):**

```java
import org.jooq.impl.DSL;

public PostDTO fetchPostWithComments(Long postId) {
    return dslContext.select(
            POSTS.TITLE,
            POSTS.CONTENT,
            DSL.multiset(
                DSL.select(COMMENTS.CONTENT, COMMENTS.USER_ID)  // Use DSL.select inside multiset
                    .from(COMMENTS)
                    .where(COMMENTS.POST_ID.eq(POSTS.POST_ID))
                    .and(COMMENTS.IS_DELETED.eq(false))
            ).convertFrom(r -> r.into(Comment.class)).as("comments")
        )
        .from(POSTS)
        .where(POSTS.POST_ID.eq(postId))
        .fetchOneInto(PostDTO.class);
}
```

**MULTISET with Single-Column Conversion (List<Long>):**

⚠️ **IMPORTANT**: When selecting a single column (like IDs), you MUST use `.convertFrom(r -> r.into(TargetType.class))` to convert jOOQ Result to Java List.

```java
public Optional<UserMemberDTO> fetchUserWithDetailsById(Long userId) {
    return dslContext.select(
            USERS.ID,
            USERS.EMAIL,
            MEMBERSHIPS.NAME,
            DSL.multiset(
                DSL.select(MEMBERSHIP_ROLES.ROLE_ID)  // Use DSL.select inside multiset
                    .from(MEMBERSHIP_ROLES)
                    .where(MEMBERSHIP_ROLES.ORG_ID.eq(MEMBERSHIPS.ORG_ID))
                    .and(MEMBERSHIP_ROLES.MEMBERSHIP_ID.eq(MEMBERSHIPS.ID))
                    .orderBy(MEMBERSHIP_ROLES.IS_PRIMARY.desc())
            ).convertFrom(r -> r.into(Long.class)).as("roleIds")  // ✓ REQUIRED for List<Long>
        )
        .from(USERS)
        .join(MEMBERSHIPS).on(MEMBERSHIPS.USER_ID.eq(USERS.ID))
        .where(USERS.ID.eq(userId))
        .fetchOptionalInto(UserMemberDTO.class);
}
```

**Key Points:**
- **Outer query**: Use `dslContext.select()` - your DSLContext instance
- **Inside multiset**: Use `DSL.select()` - static DSL class method
- **DSL class**: Use for both `DSL.multiset()` and `DSL.select()` inside multiset
- **Conversion**: Always use `.convertFrom(r -> r.into(TargetType.class))` for single-column multisets

**Why `.convertFrom()` is Required:**
- jOOQ `multiset()` returns `Result<Record1<T>>` (a jOOQ collection type)
- Java DTOs expect `List<T>` (standard Java collection)
- Without `.convertFrom()`, JWT serialization, JSON serialization, and other Java libraries will fail
- `.into(Long.class)` converts `Result<Record1<Long>>` → `List<Long>`

**Alternative: fetchGroups Pattern (for reference only, prefer MULTISET):**

```java
public List<ActorFilmographyDTO> findActorFilmography(ActorFilmographySearchOptionDTO searchOption) {
    Map<Actor, List<Film>> actorListMap = dslContext.select(ACTOR, FILM)
            .from(ACTOR)
            .join(FILM_ACTOR).on(ACTOR.ACTOR_ID.eq(FILM_ACTOR.ACTOR_ID))
            .join(FILM).on(FILM.FILM_ID.eq(FILM_ACTOR.FILM_ID))
            .where(/* conditions */)
            .fetchGroups(
                    record -> record.get(ACTOR.$name(), Actor.class),
                    record -> record.get(FILM.$name(), Film.class)
            );

    return actorListMap.entrySet().stream()
            .map(entry -> new ActorFilmographyDTO(entry.getKey(), entry.getValue()))
            .toList();
}
```

### Dynamic Queries with Condition Utilities

```java
import static com.sungbok.community.repository.util.JooqConditionUtils.eq;
import static com.sungbok.community.repository.util.JooqStringConditionUtils.*;

public List<Post> fetchByDynamicCriteria(SearchCriteria criteria) {
    return dslContext.selectFrom(POSTS)
        .where(
            eq(POSTS.STATUS, criteria.getStatus()),
            containsIfNotBlank(POSTS.TITLE, criteria.getSearch()),
            inIfNotEmpty(POSTS.CATEGORY, criteria.getCategories())
        )
        .fetchInto(Post.class);
}
```

### GROUP_CONCAT for Aggregation

```java
public List<UserWithRolesDTO> fetchUsersWithRoles() {
    return dslContext.select(
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

## Method Naming Conventions

**This project uses jOOQ conventions, NOT JPA conventions.**

### Query Methods (조회)

**fetch\***: 단일/복수 결과 조회
```java
fetchById(Long id)         // ID로 조회
fetchByEmail(String email) // 이메일로 조회
fetchAll()                 // 전체 조회
fetchAllPosts(SearchVO)    // 복수 조회
fetchUserWithDetailsById() // JOIN 조회
```

**exists\***: 존재 여부 확인
```java
existsById(Long id)
existsByEmail(String email)
```

**count\***: 개수 조회
```java
countAll()
countByStatus(String status)
```

### Mutation Methods (변경)

- **insert**: 새 레코드 삽입 (항상 RETURNING 절 사용)
- **update**: 레코드 수정 (DSLContext.update() 패턴)
- **delete**: 레코드 삭제
  - `softDelete(Long id)` - Soft delete (권장)
  - `hardDelete(Long id)` - Hard delete (주의)

### Enum Methods

- **fromCode()** - 코드로 Enum 조회

### DO NOT Use (JPA 스타일)

- `find*` - JPA 스타일, 사용하지 마세요
- `save*` - JPA 스타일, insert/update로 명확히 구분
- `remove*` - JPA 스타일, delete 사용
- `findByCode()` - Enum에서 사용하지 마세요, fromCode() 사용

### Examples

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

## DO Examples

```java
// ✓ Simple query - use DAO
public Optional<User> fetchById(Long id) {
    return Optional.ofNullable(dao.findById(id));
}

// ✓ Partial update - use DSLContext
public int incrementViewCount(Long postId) {
    return dslContext.update(POSTS)
            .set(POSTS.VIEW_COUNT, POSTS.VIEW_COUNT.add(1))
            .where(POSTS.POST_ID.eq(postId))
            .execute();
}

// ✓ Soft delete standard
public int softDelete(Long id) {
    return dslContext.update(ENTITY)
            .set(ENTITY.IS_DELETED, true)
            .set(ENTITY.MODIFIED_AT, LocalDateTime.now())
            .where(ENTITY.ID.eq(id))
            .execute();
}
```

## DON'T Examples

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

## jOOQ vs JPA - Critical Differences

**This project uses jOOQ, NOT JPA!**

### DO NOT (절대 하지 마세요)

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

### DO (올바른 jOOQ 방식)

```java
// ✓ jOOQ가 생성한 POJO 사용
import org.jooq.generated.tables.pojos.Users;
import org.jooq.generated.tables.pojos.Members;

// ✓ 관계는 명시적 JOIN으로 표현
public List<UserMemberDTO> fetchUsersWithMembers() {
    return dslContext.select(
            USERS.fields(),
            MEMBERS.fields()
        )
        .from(USERS)
        .join(MEMBERS).on(MEMBERS.USER_ID.eq(USERS.ID))  // 명시적 JOIN
        .fetchInto(UserMemberDTO.class);
}

// ✓ MULTISET으로 중첩 컬렉션 표현
public UserWithMembersDTO fetchUserWithMembers(Long userId) {
    return dslContext.select(
            USERS.fields(),
            multiset(
                dslContext.select(MEMBERS.fields())
                    .from(MEMBERS)
                    .where(MEMBERS.USER_ID.eq(USERS.ID))
            ).as("members")
        )
        .from(USERS)
        .where(USERS.ID.eq(userId))
        .fetchOneInto(UserWithMembersDTO.class);
}
```

## Key Principles

1. **Entity 클래스 직접 생성 금지**: jOOQ가 자동 생성한 POJO 사용
2. **관계 매핑 어노테이션 금지**: @OneToMany, @ManyToOne, @ManyToMany 등 사용 불가
3. **FK는 DB에서 관리**: 관계는 데이터베이스 스키마에서 정의
4. **명시적 JOIN 사용**: 코드에서 관계를 표현할 때는 JOIN 또는 MULTISET 사용
5. **Generated 코드 수정 금지**: `build/generated-src/jooq/main` 폴더의 코드는 건드리지 않음

## Why jOOQ over JPA?

- **Type Safety**: Compile-time query validation
- **Performance**: No hidden N+1 queries, explicit control
- **Transparency**: See exactly what SQL is generated
- **Flexibility**: Full SQL feature support (MULTISET, window functions, CTEs)
- **No Magic**: No lazy loading, no proxy objects, no automatic dirty checking

## Database Access Pattern

Use jOOQ DSLContext directly in repositories. Generated jOOQ classes are in the `org.jooq.generated` package with 'J' prefix:
- Tables: `JUsers`, `JMembers`, `JPosts`, etc.
- Records: `JUsersRecord`, `JMembersRecord`, etc.
- DAOs: `JUsersDao`, `JMembersDao`, etc.

## Reference

For detailed jOOQ patterns and performance analysis.
