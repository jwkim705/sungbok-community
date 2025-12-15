# Multi-Tenancy Architecture (Multi-App Platform with Guest Mode)

**Platform Model:** Notion-style multi-app SaaS platform supporting multiple app types (Church, Cafe, Company, School), with multiple organizations per app type, guest mode for onboarding, and approval-based membership.

## Architecture Overview

### Hierarchy

```
Platform
├── App Types (4)
│   ├── Church Community (app_type_id=1)
│   ├── Cafe Community (app_type_id=2)
│   ├── Company Community (app_type_id=3)
│   └── School Community (app_type_id=4)
└── Organizations (Many)
    ├── Church A (org_id=1, app_type_id=1)
    ├── Church B (org_id=2, app_type_id=1)
    ├── Cafe A (org_id=3, app_type_id=2)
    └── ...

User (user@example.com, platform-level)
└── Memberships
    ├── Church A (org_id=1, role_id=2 "리더", status=APPROVED)
    ├── Cafe A (org_id=3, role_id=1 "Member", status=APPROVED)
    └── Company A (org_id=5, role_id=3 "팀장", status=PENDING)
```

### Tenant Isolation Strategy

- **Pattern:** Shared Database, Shared Schema with `org_id` discriminator
- **Tenant Identifier:** `org_id` (BIGINT, references `organizations.org_id`)
- **Isolation Level:** Row-level security via application logic (ThreadLocal + jOOQ)
- **Multi-Level:** Platform-level (users) + Organization-scoped (memberships, posts, etc.)

## Table Classification

### Platform-Level Tables (NO org_id filtering)

- `users` - Platform-wide user accounts
- `app_types` - App type definitions

**CRITICAL:** These tables NEVER use `orgIdCondition()`. Query directly by email/id.

```java
// Platform-level query example
public Optional<Users> fetchByEmail(String email) {
    return dsl.selectFrom(USERS)
            .where(USERS.EMAIL.eq(email))  // NO orgIdCondition
            .and(USERS.IS_DELETED.eq(false))
            .fetchOptionalInto(Users.class);
}
```

### Organization-Scoped Tables (REQUIRES org_id filtering)

12 tables include `org_id` for tenant isolation:
```
memberships, oauth_accounts, posts, comments, post_likes, post_youtube,
files, attendance, family_relations, groups, group_membership, organizations
```

**CRITICAL:** These tables ALWAYS use `orgIdCondition(TABLE.ORG_ID)`.

### Shared Reference Tables (NO org_id)

- `departments` - Department hierarchy
- `posts_categories` - Post category definitions

## JWT Structure

### Access Token (Authenticated User)

```json
{
  "sub": "user@example.com",
  "userId": 123,
  "orgId": 1,        // Currently active organization
  "appTypeId": 1,    // Church, Cafe, Company, or School
  "roleIds": [5, 7, 9],  // User's roles in this organization (배열 - 다중 역할 지원)
  "iss": "sungbok-community",
  "iat": 1234567890,
  "exp": 1234568790  // 15 minutes
}
```

### Refresh Token

Contains only minimal claims (email, issuer) for security.

## Tenant Resolution (Dual Mode)

### 1. Authenticated Mode (JWT)

`JwtAuthenticationFilter` extracts `orgId` from JWT and sets `TenantContext`:

```java
Claims claims = jwtTokenProvider.getClaimsFromToken(token);
Long orgId = claims.get("orgId", Long.class);
TenantContext.setOrgId(orgId);
```

**JWT's orgId is authoritative** - no header override allowed when authenticated.

### 2. Guest Mode (X-Org-Id Header)

`TenantResolver` extracts org from header for unauthenticated requests:

```java
String headerOrgId = request.getHeader("X-Org-Id");
if (headerOrgId != null) {
    Long orgId = Long.parseLong(headerOrgId);
    // Verify org exists and is_public = true
    Organizations org = organizationsRepository.fetchById(orgId);
    if (org != null && org.getIsPublic()) {
        TenantContext.setOrgId(orgId);
    }
}
```

**Guest mode allows:**
- `GET /api/organizations` - List public orgs
- `GET /api/posts` - Read posts (read-only)
- `GET /api/comments` - Read comments (read-only)

**Guest mode denies:**
- `POST`, `PUT`, `DELETE` - All write operations (403 Forbidden)

## Repository Layer Filtering Pattern

### Organization-Scoped Repositories (12 tables)

**ALWAYS use `orgIdCondition()`:**

```java
import static com.sungbok.community.repository.util.JooqTenantConditionUtils.orgIdCondition;

public Optional<Posts> fetchById(Long postId) {
    return dsl.selectFrom(POSTS)
            .where(orgIdCondition(POSTS.ORG_ID))  // REQUIRED
            .and(POSTS.POST_ID.eq(postId))
            .and(POSTS.IS_DELETED.eq(false))
            .fetchOptionalInto(Posts.class);
}
```

### Platform-Level Repositories (users, app_types)

**NEVER use `orgIdCondition()`:**

```java
// NO org_id filtering for users table
public Optional<Users> fetchById(Long userId) {
    return dsl.selectFrom(USERS)
            .where(USERS.ID.eq(userId))
            .and(USERS.IS_DELETED.eq(false))
            .fetchOptionalInto(Users.class);
}
```

### JOIN Pattern (User + Membership)

When joining platform-level (users) with org-scoped (memberships):

```java
public Optional<UserMemberDTO> fetchUserWithDetailsById(Long userId) {
    return dsl.select(
                    MEMBERSHIPS.ORG_ID,  // From memberships, not users
                    USERS.ID,
                    USERS.EMAIL,
                    MEMBERSHIPS.NAME,
                    MEMBERSHIPS.ROLE_ID
            )
            .from(USERS)
            .join(MEMBERSHIPS).on(MEMBERSHIPS.USER_ID.eq(USERS.ID))
            .where(orgIdCondition(MEMBERSHIPS.ORG_ID))  // Filter on memberships
            .and(USERS.ID.eq(userId))
            .fetchOptionalInto(UserMemberDTO.class);
}
```

## INSERT Patterns

### Organization-Scoped INSERT

**Force org_id from TenantContext:**

```java
public Memberships insert(Memberships membership) {
    Long orgId = TenantContext.getRequiredOrgId();
    membership.setOrgId(orgId);  // Force current tenant

    MembershipsRecord record = dsl.newRecord(MEMBERSHIPS, membership);
    return dsl.insertInto(MEMBERSHIPS)
            .set(record)
            .returning()
            .fetchOneInto(Memberships.class);
}
```

### Platform-Level INSERT

**NO org_id:**

```java
public Users insert(Users user) {
    // No org_id for users table
    UsersRecord record = dsl.newRecord(USERS, user);
    return dsl.insertInto(USERS)
            .set(record)
            .returning()
            .fetchOneInto(Users.class);
}
```

## UPDATE/DELETE Patterns

### Organization-Scoped UPDATE

```java
public int update(Posts post) {
    return dsl.update(POSTS)
            .set(POSTS.TITLE, post.getTitle())
            .set(POSTS.MODIFIED_AT, LocalDateTime.now())
            .where(orgIdCondition(POSTS.ORG_ID))  // Tenant isolation
            .and(POSTS.POST_ID.eq(post.getPostId()))
            .execute();
}
```

### SOFT DELETE (Organization-Scoped)

```java
public int softDelete(Long postId) {
    return dsl.update(POSTS)
            .set(POSTS.IS_DELETED, true)
            .set(POSTS.MODIFIED_AT, LocalDateTime.now())
            .where(orgIdCondition(POSTS.ORG_ID))  // Tenant isolation
            .and(POSTS.POST_ID.eq(postId))
            .execute();
}
```

## TenantContext (ThreadLocal)

`TenantContext` stores the current request's `org_id` using ThreadLocal:

```java
// Automatically set by JwtAuthenticationFilter or TenantResolver
Long orgId = TenantContext.getRequiredOrgId();  // Throws if not set
Long orgId = TenantContext.getOrgId();          // Returns null if not set

// Cleanup is handled automatically in filter's finally block
```

**Important:**
- NEVER manually set `TenantContext` in application code
- Always use `TenantContext.getRequiredOrgId()` in org-scoped repositories
- Platform-level repositories (users) don't use TenantContext

## Guest Mode Flow

### Mobile App Launch (First Time)

```javascript
// 1. Show org selection screen
const orgs = await fetch('https://api.sungbok.kr/api/organizations');

// 2. User selects "Church A"
await AsyncStorage.setItem('selectedOrgId', '1');

// 3. All API calls include X-Org-Id header
fetch('https://api.sungbok.kr/api/posts', {
  headers: { 'X-Org-Id': '1' }
});
```

### Guest Permissions

```
✅ Allowed (GET only):
- /api/organizations (list all public orgs)
- /api/organizations/{orgId} (get org details)
- /api/app-types (list all app types)
- /api/app-types/{appTypeId}/organizations (filter orgs by app type)
- /api/posts (read posts in selected org)
- /api/posts/{postId} (read post details)
- /api/posts/{postId}/comments (read comments)

❌ Denied (403 Forbidden):
- POST /api/posts (create post)
- All write operations
```

### Join Organization Flow

```
Guest → Click "Join" → Signup/Login → Request created → Admin approval → Active membership

Membership status:
1. PENDING: User submitted join request
2. APPROVED: Admin approved, user is now active member
3. REJECTED: Admin rejected, cannot access (but can re-request)
```

## Security Considerations

1. **Every org-scoped query MUST filter by org_id** - no exceptions
2. **No cross-tenant queries allowed** - enforced at application level
3. **JWT validation verifies orgId matches database** - prevents token tampering
4. **ThreadLocal cleanup prevents memory leaks** - always cleared in filter's finally block
5. **INSERT operations force org_id** - prevents malicious clients from specifying wrong tenant
6. **Guest mode verified** - X-Org-Id header checked against `organizations.is_public`
7. **Platform-level tables (users) are shared** - email uniqueness enforced globally

## Testing Multi-Tenancy

When writing tests that involve tenant-scoped data:

```java
@BeforeEach
void setUp() {
    TenantContext.setOrgId(1L);  // Set test tenant
}

@AfterEach
void tearDown() {
    TenantContext.clear();  // Cleanup
}

@Test
void testCrossTenantIsolation() {
    // Create post in org 1
    TenantContext.setOrgId(1L);
    Posts post1 = postsRepository.insert(new Posts().setTitle("Org 1"));

    // Switch to org 2
    TenantContext.setOrgId(2L);
    Optional<Posts> post = postsRepository.fetchById(post1.getPostId());

    // Should NOT find post1 (cross-tenant isolation)
    assertThat(post).isEmpty();
}
```

## Role-Based Permissions

Each organization has custom roles with API-level permissions:

```sql
-- role_permissions table
role_id | resource  | action | allowed
--------|-----------|--------|--------
1       | posts     | create | true
1       | posts     | delete | false   -- 성도: can create but not delete
2       | posts     | delete | true    -- 리더: can delete
3       | users     | read   | true    -- 마을장: can manage users
```

**Implementation:** Spring Security `@PreAuthorize` with custom `PermissionChecker` component.

See plan file for full permission system design.

## Key Differences from Traditional Multi-Tenancy

1. **Two-Level Structure:** Platform (users) + Organizations (memberships, posts)
2. **Multi-App Types:** One platform, multiple app types (Church, Cafe, Company)
3. **Multi-Membership:** One user can belong to many organizations across different app types
4. **Guest Mode:** Public preview before signup (mobile-first onboarding)
5. **Approval Workflow:** Admin-controlled membership activation
6. **Custom Roles per Org:** Each organization defines its own role hierarchy

This is **Notion model** (multiple workspace types), NOT Slack model (single app type).
