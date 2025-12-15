package com.sungbok.community.repository;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.generated.tables.daos.RolePermissionsDao;
import org.jooq.generated.tables.pojos.RolePermissions;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static org.jooq.generated.Tables.ROLE_PERMISSIONS;

/**
 * 역할 권한 데이터 접근 Repository
 * 하이브리드 DAO + DSL 패턴 사용
 */
@Repository
public class RolePermissionsRepository {

    private final DSLContext dsl;
    private final RolePermissionsDao dao;

    public RolePermissionsRepository(DSLContext dsl, Configuration configuration) {
        this.dsl = dsl;
        this.dao = new RolePermissionsDao(configuration);
    }

    /**
     * 역할 ID, 리소스, 액션으로 권한 조회
     *
     * @param roleId 역할 ID
     * @param resource 리소스 (posts, comments, users 등)
     * @param action 액션 (create, read, update, delete)
     * @return 권한 Optional (없으면 빈 Optional)
     */
    public Optional<RolePermissions> fetchByRoleAndResourceAndAction(Long roleId, String resource, String action) {
        return dsl.selectFrom(ROLE_PERMISSIONS)
                .where(ROLE_PERMISSIONS.ROLE_ID.eq(roleId))
                .and(ROLE_PERMISSIONS.RESOURCE.eq(resource))
                .and(ROLE_PERMISSIONS.ACTION.eq(action))
                .fetchOptionalInto(RolePermissions.class);
    }
}
