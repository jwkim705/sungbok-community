# Build, Testing, and Deployment

## Local Development Setup

### Start Infrastructure (PostgreSQL + Valkey)

```bash
# Set required environment variables first
export POSTGRES_USER=admin
export POSTGRES_PASSWORD=1234
export POSTGRES_DB=community
export VALKEY_PASSWORD=1234

# Use docker-compose or podman-compose
podman compose up -d postgres valkey
```

### Build and Run

```bash
./gradlew clean build    # Clean build with tests
./gradlew build          # Build without clean
./gradlew bootRun        # Run application (local profile)
```

### Generate jOOQ Code

```bash
# Requires PostgreSQL running locally
./gradlew generateJooq
```

**Requirements:**
- PostgreSQL running on port 5432
- Database credentials: admin/1234
- Database name: community
- Environment variables can override via `DB_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`

## Testing

### Run Tests

```bash
./gradlew test                                    # Run all tests
./gradlew test --tests UserControllerTest         # Run specific test class
./gradlew test --tests UserControllerTest.signup  # Run specific test method
```

### Test Configuration

Tests use:
- `@SpringBootTest` with `@AutoConfigureMockMvc` for integration tests
- `@ActiveProfiles("test")` to use `application-test.yml`
- `@MockitoSpyBean` for partial mocking
- Spring REST Docs with `restdocs-api-spec` for API documentation generation

Test profile uses hardcoded credentials (test data) rather than environment variables.

## API Documentation

```bash
./gradlew openapi3       # Generate OpenAPI 3 spec from REST Docs
# Access Swagger UI at: http://localhost:8080/api/swagger-ui.html
```

## Environment Variables

### Required for local development

```bash
POSTGRES_USER=admin
POSTGRES_PASSWORD=1234
POSTGRES_DB=community
VALKEY_PASSWORD=1234
```

### Required for OAuth2 (optional for basic development)

```bash
GOOGLE_CLIENT_ID=<your-google-client-id>
GOOGLE_CLIENT_SECRET=<your-google-client-secret>
KAKAO_CLIENT_ID=<your-kakao-client-id>
KAKAO_CLIENT_SECRET=<your-kakao-client-secret>
NAVER_CLIENT_ID=<your-naver-client-id>
NAVER_CLIENT_SECRET=<your-naver-client-secret>
```

### Optional for deployment

```bash
DB_URL=jdbc:postgresql://host:5432/dbname
PGADMIN_DEFAULT_EMAIL=<email>
PGADMIN_DEFAULT_PASSWORD=<password>
```

## Deployment

The project supports blue-green deployment with separate Docker Compose files in the `docker/` directory. The application runs on context path `/api` (configured in `application.yml`).

**Production URL:** `https://sungbok.p-e.kr/api`

## Important Notes

- **No Session Management:** Application is fully stateless, using JWT tokens
- **Java 25:** Uses latest Java features and Spring Boot 4.0
- **jOOQ First:** This project uses jOOQ, not JPA/Hibernate
- **Log4j2:** Using Log4j2 instead of Logback (Spring Boot default is excluded)
- **XSS Protection:** Custom `HtmlCharacterEscapes` for JSON serialization
