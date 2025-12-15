# Architecture

## Package Structure

```
com.sungbok.community/
├── controller/          # REST endpoints
├── service/
│   ├── get/            # Query services (read operations)
│   ├── change/         # Command services (write operations)
│   └── oauth/          # OAuth2 social login services
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

## Authentication Flow

1. **Form Login & OAuth2:** Both generate JWT tokens via `CustomAuthenticationSuccessHandler`
2. **JWT Filter (`JwtAuthenticationFilter`):** Validates tokens on every request before Spring Security filters
3. **Stateless Sessions:** `SessionCreationPolicy.STATELESS` - no server-side sessions
4. **Token Storage:** Refresh tokens stored in Valkey with email as key
5. **Token Refresh:** `/auth/refresh` endpoint accepts refresh token and returns new access token

## Service Layer Pattern

Services are organized by operation type:
- **Get Services:** Read-only operations, queries
- **Change Services:** Write operations (create, update, delete)

This separation aligns with CQRS principles and makes transaction boundaries clearer.

## CORS Configuration

Allowed origins configured in `application.yml`:
- `http://localhost:3000` (React/Next.js)
- `http://localhost:19006` (Expo)

## Frontend Environment

### Mobile Application

- **Framework:** Expo (React Native)
- **Dev Server:** http://localhost:19006
- **API Integration:** REST API calls to `/api` context path
- **Authentication:** JWT Bearer tokens

**Note**: 웹 애플리케이션은 현재 없음. 모바일 앱만 개발 중.

## Error Handling

Global exception handling via `@RestControllerAdvice` in `ErrorHandlerAdvice`.

Custom exceptions:
- `AlreadyExistException`
- `BadRequestException`
- `DataNotFoundException`
- `NotFoundException`
- `NotMatchPwdException`
- `UnAuthorizedException`
- `SystemException`

## jOOQ Code Generation

The project uses a custom generator strategy (`JPrefixGeneratorStrategy` from `jooq-custom` submodule) that prefixes generated classes with 'J'. Generated code is placed in `build/generated-src/jooq/main` and included in source sets.

**Important:** jOOQ code generation requires:
- PostgreSQL running locally on port 5432
- Database credentials: admin/1234
- Database name: community
- Environment variables can override defaults via `DB_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`

Generated jOOQ classes are in the `org.jooq.generated` package with 'J' prefix:
- Tables: `JUsers`, `JMembers`, `JPosts`, etc.
- Records: `JUsersRecord`, `JMembersRecord`, etc.
- DAOs: `JUsersDao`, `JMembersDao`, etc.

## Multi-Module Structure

The project includes a `jooq-custom` submodule for custom jOOQ code generation strategies. When modifying jOOQ generation behavior, update the submodule at `jooq-custom/`.
