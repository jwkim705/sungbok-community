# CLAUDE.md

This file provides essential guidance to Claude Code when working with this repository.

## Project Overview

Spring Boot 4.0 SaaS community platform with multi-tenancy support, JWT authentication, OAuth2 social login, and PostgreSQL database using jOOQ for type-safe SQL queries.

**Key Stack:**
- Java 25 (Spring Boot 4.0.0)
- jOOQ 3.20.9 (NOT JPA)
- PostgreSQL 18.1 + Valkey (Redis fork)
- JWT ES256 (ECDSA P-256)
- OAuth2 (Google, Kakao, Naver)
- Context path: `/api`

**Frontend:**
- Mobile: Expo (React Native, localhost:19006)

## Quick Start

**Infrastructure:**
```bash
export POSTGRES_USER=admin POSTGRES_PASSWORD=1234 POSTGRES_DB=community VALKEY_PASSWORD=1234
podman compose up -d postgres valkey
```

**Build & Run:**
```bash
./gradlew clean build    # Build with tests
./gradlew bootRun        # Run application
./gradlew generateJooq   # Generate jOOQ code (requires PostgreSQL)
```

📖 **Details:** See `agent_docs/setup.md` for testing, API docs, and deployment

## Core Architecture

### Package Structure

```
com.sungbok.community/
├── controller/          # REST endpoints
├── service/
│   ├── get/            # Query services (read-only)
│   ├── change/         # Command services (write operations)
│   └── oauth/          # OAuth2 social login services
├── repository/         # jOOQ repositories (data access)
└── security/           # JWT, OAuth2, filters
```

📖 **Details:** See `agent_docs/architecture.md` for full structure and authentication flow

### jOOQ Repository Patterns

**CRITICAL RULES:**

1. **Use jOOQ, NOT JPA**
   - No `@Entity`, `@OneToMany`, `@ManyToOne` annotations
   - No `save()`, `findById()` methods
   - Use `fetch*()`, `insert()`, `update()` instead

2. **Constructor Standard:** `(DSLContext dslContext, Configuration configuration)`
   - Use `dslContext` (clear and explicit variable name)

3. **Method Naming:**
   - ✓ `fetchById()`, `fetchByEmail()` (NOT `findById()`)
   - ✓ `insert()`, `update()` (NOT `save()`)
   - ✓ `softDelete()`, `hardDelete()` (NOT `remove()`)

4. **Insert Pattern:**
   - POJO 반환 → DAO 사용: `dao.insert(pojo)`
   - PK만 반환 → `.values()` + `returningResult(PK)`
   - DTO 변환 → `.values()` + `returning(fields())`
   - ❌ 안티패턴: `.set(dsl.newRecord())`, 수동 POJO 생성 + newRecord

5. **Repository = Data Access Only**
   - No business logic
   - No encoders, validators
   - Return `Optional<>` for single results, `List<>` for collections

6. **Update Pattern:**
   - Single/few fields → `DSLContext.update()`
   - Batch updates → `UpdatableRecord` + `batchUpdate()`
   - Upsert → `Record.store()`

7. **MULTISET Pattern for Nested Collections:**
   - Use `DSL.multiset()` for nested data (12x faster than fetchGroups)
   - **CRITICAL**: Always use `.convertFrom(r -> r.into(TargetType.class))` to convert jOOQ Result to Java List
   - Example:
   ```java
   DSL.multiset(
       DSL.select(MEMBERSHIP_ROLES.ROLE_ID)
           .from(MEMBERSHIP_ROLES)
           .where(...)
   ).convertFrom(r -> r.into(Long.class)).as("roleIds")  // Required!
   ```
   - Without `.convertFrom()`, JWT serialization and JSON serialization will fail

📖 **Details:** See `agent_docs/jooq-patterns.md` for templates, advanced patterns (MULTISET, dynamic queries), and DO/DON'T examples

### Multi-Tenancy Architecture (Multi-App Platform)

**Platform Model:** Notion-style with multiple app types (Church, Cafe, Company, School), each with multiple organizations.

**CRITICAL RULES:**

1. **Platform-Level vs Organization-Scoped Tables**
   - **Platform-Level (NO org_id):** `users`, `app_types`
   - **Organization-Scoped (12 tables):** `memberships`, `oauth_accounts`, `posts`, `comments`, `post_likes`, `post_youtube`, `files`, `attendance`, `family_relations`, `groups`, `group_membership`, `organizations`
   - **Shared Reference:** `departments`, `posts_categories`

2. **Every org-scoped query MUST use `orgIdCondition(TABLE.ORG_ID)`**
   ```java
   import static com.sungbok.community.repository.util.JooqTenantConditionUtils.orgIdCondition;

   .where(orgIdCondition(POSTS.ORG_ID))  // REQUIRED for org-scoped tables
   ```

3. **Platform-Level Queries (Users):**
   ```java
   // NO org_id filtering for users table
   dsl.selectFrom(USERS).where(USERS.EMAIL.eq(email))
   ```

4. **TenantContext Management:**
   - Auto-set by `JwtAuthenticationFilter` (authenticated) or `TenantResolver` (guest)
   - Guest mode: `X-Org-Id` header → TenantContext (read-only access)
   - JWT mode: JWT's `orgId` claim → TenantContext (full access based on role)

5. **INSERT Pattern (Organization-Scoped):**
   ```java
   Long orgId = TenantContext.getRequiredOrgId();
   membership.setOrgId(orgId);  // Force current tenant
   ```

6. **Guest Mode Support:**
   - `GET /api/organizations` - List public organizations
   - `GET /api/organizations/{orgId}` - Get organization details
   - `GET /api/app-types` - List app types
   - `GET /api/app-types/{id}/organizations` - Filter organizations by app type
   - `GET /api/posts`, `/api/posts/{id}` - Read-only posts with X-Org-Id header
   - `GET /api/posts/{postId}/comments` - Read comments
   - All write operations require authentication

📖 **Details:** See `agent_docs/multi-tenancy.md` for architecture, JWT structure, guest mode, role-based permissions

### Security & JWT

**CRITICAL RULES:**

1. **JWT Algorithm:** ES256 (ECDSA P-256)
   - Private key: `src/main/resources/keys/jwt-private.pem`
   - Public key: `src/main/resources/keys/jwt-public.pem`
   - ⚠️ **NEVER commit keys to Git**

2. **Token Lifetimes:**
   - Access token: 15 minutes
   - Refresh token: 7 days

3. **JWT Structure (Access Token):**
   ```json
   {
     "sub": "user@example.com",
     "userId": 123,
     "orgId": 1,        // Currently active organization
     "appTypeId": 1,    // Church, Cafe, Company, or School
     "roleIds": [5, 7, 9]  // User's roles in this org (for permissions)
   }
   ```

4. **Guest Mode:** X-Org-Id header for unauthenticated read-only access

📖 **Details:** See `agent_docs/security-jwt.md` for key generation, OAuth2 setup, production recommendations

## Code Documentation

- **JavaDoc:** Simple Korean, technical terms in English
- **Class level:** Purpose and pattern (e.g., "사용자 데이터 접근 Repository")
- **Method level:** What it does, `@param`, `@return`, `@throws`

📖 **Details:** See `agent_docs/code-documentation.md` for examples and best practices

## References

- **Detailed Patterns:** `agent_docs/` directory
- **jOOQ Examples:** 
- **Production URL:** `https://sungbok.p-e.kr/api`

- Use Korean to communicate with users