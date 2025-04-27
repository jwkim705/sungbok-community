package com.sungbok.community.repository.departmentRoles;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.generated.tables.pojos.UserDepartmentRoles;
import org.jooq.generated.tables.records.UserDepartmentRolesRecord;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static org.jooq.generated.Tables.USER_DEPARTMENT_ROLES;

@Repository
@RequiredArgsConstructor
public class DeptRolesRepository {

    private final DSLContext dsl;

    public Optional<UserDepartmentRoles> findByUserId(Long userId) {
        return dsl.select(
                USER_DEPARTMENT_ROLES,
                USER_DEPARTMENT_ROLES.roles(),
                USER_DEPARTMENT_ROLES.departments()
        )
                .from(USER_DEPARTMENT_ROLES)
                .join(USER_DEPARTMENT_ROLES.roles())
                .join(USER_DEPARTMENT_ROLES.departments())
                .where(USER_DEPARTMENT_ROLES.USER_ID.eq(userId))
                .fetchOptionalInto(UserDepartmentRoles.class);
    }

    public UserDepartmentRoles save(UserDepartmentRoles userDepartmentRoles) {

        UserDepartmentRolesRecord record = dsl.newRecord(USER_DEPARTMENT_ROLES, userDepartmentRoles);

        record.setUserId(userDepartmentRoles.getUserId());
        record.setDepartmentId(userDepartmentRoles.getDepartmentId());
        record.setRoleId(userDepartmentRoles.getRoleId());
        record.store();

        return record.into(UserDepartmentRoles.class);

    }

}