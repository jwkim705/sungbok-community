# Roles Policy (역할 정책)

## 개요

이 프로젝트는 **동적 역할(Dynamic Roles)** 시스템을 사용합니다.
- 정적 Enum 없음 (UserRole enum 제거됨)
- 조직별 커스텀 역할 (roles 테이블)
- 다중 역할 지원 (한 유저가 여러 역할 가능)

## 데이터베이스 구조

### roles 테이블 (조직별 역할 정의)
```sql
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT,
    name VARCHAR(100) NOT NULL,  -- "성도", "리더", "교사" 등
    level INT DEFAULT 1,         -- 1=낮음, 3=높음
    description TEXT,
    ...
);
```

### membership_roles 테이블 (N:M 관계)
```sql
CREATE TABLE membership_roles (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL,
    membership_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,  -- 대표 역할
    ...
    UNIQUE (org_id, membership_id, role_id)
);
```

## JWT 구조

```json
{
  "userId": 123,
  "orgId": 1,
  "roleIds": [5, 7, 9],  // 배열
  "appTypeId": 1,
  "name": "홍길동"
}
```

## Spring Security 통합

### PrincipalDetails.getAuthorities()
```java
// roleIds 배열을 순회하여 모든 권한 추가
for (Long roleId : roleIds) {
    authorities.add(new SimpleGrantedAuthority("ROLE_" + roleId));
}
```

### PermissionChecker.hasPermission()
```java
// 하나라도 권한이 있으면 true
for (Long roleId : roleIds) {
    if (hasPermission(roleId, resource, action)) {
        return true;
    }
}
```

## 기본 역할 할당 규칙

**signup 시 자동 할당**:
- 조직의 `level=1` 역할을 기본으로 할당
- `is_primary=true`로 설정
- 예: "성도" 역할 (가장 낮은 권한)

```java
Roles defaultRole = rolesRepository.fetchByOrgIdAndLevel(orgId, 1)
        .orElseThrow(() -> new DataNotFoundException(...));

membershipRolesRepository.assignRole(
    membershipId,
    defaultRole.getId(),
    true,  // primary
    assignedBy
);
```

## 프론트엔드 사용

### 역할 정보 조회
```typescript
// JWT에서 roleIds 추출
const roleIds = jwtPayload.roleIds;  // [5, 7, 9]

// 역할 목록 API 호출
const roles = await fetch('/api/roles').then(r => r.json());

// roleIds로 역할 이름 매핑
const userRoles = roles.data.filter(r => roleIds.includes(r.id));
console.log(userRoles.map(r => r.name));  // ["리더", "교사"]
```

## 관리자 기능 (향후 별도 웹)

관리자 웹 프로젝트에서 구현 예정:
- 역할 추가/삭제/수정
- 사용자에게 역할 할당/해제
- 대표 역할 변경

## 중요 규칙

1. **절대 UserRole Enum 사용 금지**
   - 과거 코드에서 UserRole enum 제거됨
   - roles 테이블만 사용

2. **다중 역할 지원 필수**
   - 한 유저가 여러 역할 가질 수 있음
   - roleIds는 항상 배열로 처리

3. **level 필드 의미**
   - 1 = 가장 낮은 권한 (기본 역할)
   - 2 = 중간 권한
   - 3 = 높은 권한

4. **is_primary 필드 의미**
   - UI에서 대표 역할 표시용
   - 한 멤버십당 1개만 true
   - JWT에서는 primary가 첫번째로 정렬됨

## Repository 패턴

### MembershipRolesRepository
```java
// 역할 ID 목록 조회 (JWT 생성용)
List<Long> roleIds = membershipRolesRepository.fetchRoleIdsByMembershipId(membershipId);

// 역할 할당
membershipRolesRepository.assignRole(membershipId, roleId, isPrimary, assignedBy);
```

### RolesRepository
```java
// 기본 역할 조회
Optional<Roles> defaultRole = rolesRepository.fetchByOrgIdAndLevel(orgId, 1);

// 모든 역할 조회 (앱 API용)
List<Roles> roles = rolesRepository.fetchAllByOrgId(orgId);
```

## 마이그레이션 히스토리

- **V1 (초기)**: memberships.role_id 제거, membership_roles 테이블 추가
- UserRole enum 완전 삭제
- JWT 구조 변경 (roleId → roleIds)
