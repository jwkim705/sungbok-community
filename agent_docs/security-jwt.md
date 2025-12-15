# Security & JWT Configuration

## JWT Configuration

- **Algorithm**: ES256 (ECDSA P-256)
- **Private key**: `src/main/resources/keys/jwt-private.pem`
- **Public key**: `src/main/resources/keys/jwt-public.pem`
- **Access token**: 15 minutes
- **Refresh token**: 7 days
- **Header**: `Authorization: Bearer <token>`

## Generating JWT Keys

⚠️ **보안 주의사항**: JWT 키 파일은 민감한 정보입니다. 절대 Git에 커밋하지 마세요!

```bash
# 키 파일 생성 위치로 이동
cd src/main/resources/keys

# 1. EC private key 생성 (P-256 curve)
openssl ecparam -name prime256v1 -genkey -noout -out jwt-private-ec.pem

# 2. PKCS8 format으로 변환
openssl pkcs8 -topk8 -nocrypt -in jwt-private-ec.pem -out jwt-private.pem

# 3. Public key 추출
openssl ec -in jwt-private-ec.pem -pubout -out jwt-public.pem

# 4. 임시 파일 삭제
rm jwt-private-ec.pem

# 5. 파일 권한 설정 (중요!)
chmod 600 jwt-private.pem  # 소유자만 읽기/쓰기
chmod 644 jwt-public.pem   # 모두 읽기, 소유자만 쓰기
```

## Key File Management Principles

1. ✅ `.gitignore`에 `src/main/resources/keys/*.pem` 포함 (이미 설정됨)
2. ✅ `.env.example` 참고하여 로컬 `.env` 파일 생성
3. ❌ 키 파일을 Git에 커밋하지 말 것
4. ❌ 키 파일을 Slack, 이메일 등으로 공유하지 말 것
5. ⚠️ 키가 유출된 경우 즉시 새 키 생성 및 배포

## Production Environment Recommendations

- 외부 비밀 관리 시스템 사용 (AWS Secrets Manager, HashiCorp Vault, Azure Key Vault)
- 환경 변수로 절대 경로 지정: `JWT_PRIVATE_KEY_PATH=file:/etc/secrets/jwt-private.pem`
- 키 로테이션 정책 수립 (예: 90일마다 갱신)

## Git History Cleanup (If Keys Leaked)

키 파일이 이미 Git에 커밋된 경우, history에서 완전히 제거해야 합니다:

### Option 1: git-filter-repo (권장, 빠르고 안전)

```bash
# 설치: brew install git-filter-repo (macOS) 또는 pip install git-filter-repo

git filter-repo --path src/main/resources/keys/jwt-private.pem --invert-paths
git filter-repo --path src/main/resources/keys/jwt-public.pem --invert-paths
```

### Option 2: BFG Repo Cleaner

```bash
# 다운로드: https://recode-repo-cleaner.github.io/
java -jar bfg.jar --delete-files jwt-private.pem
java -jar bfg.jar --delete-files jwt-public.pem
git reflog expire --expire=now --all
git gc --prune=now --aggressive
```

### Force push (협업자들에게 알림 필요)

```bash
git push origin --force --all
git push origin --force --tags
```

**주의사항:**
- History 재작성 후 모든 협업자는 저장소를 다시 clone 해야 합니다
- Force push 전에 백업 생성 권장
- GitHub/GitLab의 경우 Settings에서 "Protected branches" 확인

## Public Endpoints

- `/health-check`
- `/users/signup`
- `GET /posts`, `GET /posts/**`
- `/auth/**` (login, token refresh)
- `/oauth2/**`, `/login/oauth2/code/**`
- API docs: `/v3/api-docs/**`, `/swagger-ui/**`

## OAuth2 Configuration

### Google OAuth2

- Client ID: Set via `GOOGLE_CLIENT_ID` environment variable
- Client Secret: Set via `GOOGLE_CLIENT_SECRET` environment variable
- Redirect URI: `{baseUrl}/login/oauth2/code/google`

### Kakao OAuth2

- Client ID: Set via `KAKAO_CLIENT_ID` environment variable
- Client Secret: Set via `KAKAO_CLIENT_SECRET` environment variable
- Redirect URI: `{baseUrl}/login/oauth2/code/kakao`

### Naver OAuth2

- Client ID: Set via `NAVER_CLIENT_ID` environment variable
- Client Secret: Set via `NAVER_CLIENT_SECRET` environment variable
- Redirect URI: `{baseUrl}/login/oauth2/code/naver`

## Required Environment Variables

**For local development:**
```bash
POSTGRES_USER=admin
POSTGRES_PASSWORD=1234
POSTGRES_DB=community
VALKEY_PASSWORD=1234
```

**For OAuth2 (optional for basic development):**
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
