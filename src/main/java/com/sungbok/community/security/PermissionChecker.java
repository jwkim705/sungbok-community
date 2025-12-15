package com.sungbok.community.security;

import com.sungbok.community.repository.RolePermissionsRepository;
import com.sungbok.community.security.model.PrincipalDetails;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Spring Security에서 사용하는 권한 체크 컴포넌트
 * @PreAuthorize 어노테이션에서 SpEL로 호출됩니다.
 *
 * 사용 예시:
 * @PreAuthorize("@permissionChecker.hasPermission(authentication, 'posts', 'create')")
 */
@Component("permissionChecker")
public class PermissionChecker {

    private final RolePermissionsRepository rolePermissionsRepository;

    public PermissionChecker(RolePermissionsRepository rolePermissionsRepository) {
        this.rolePermissionsRepository = rolePermissionsRepository;
    }

    /**
     * Spring Security Authentication으로 권한 체크
     * 다중 역할 지원 - 하나라도 권한이 있으면 true
     *
     * @param authentication Spring Security의 Authentication 객체
     * @param resource API 리소스 (posts, comments, users 등)
     * @param action CRUD 액션 (create, read, update, delete)
     * @return 권한 있으면 true, 없으면 false
     */
    public boolean hasPermission(Authentication authentication, String resource, String action) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // JWT에서 roleIds 추출
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof PrincipalDetails principalDetails)) {
            return false;
        }

        List<Long> roleIds = principalDetails.getRoleIds();

        if (roleIds == null || roleIds.isEmpty()) {
            return false;
        }

        // 모든 역할을 순회하여 하나라도 권한이 있으면 true
        for (Long roleId : roleIds) {
            boolean hasPermission = rolePermissionsRepository
                    .fetchByRoleAndResourceAndAction(roleId, resource, action)
                    .map(permission -> permission.getAllowed() != null && permission.getAllowed())
                    .orElse(false);

            if (hasPermission) {
                return true;
            }
        }

        return false;
    }
}
