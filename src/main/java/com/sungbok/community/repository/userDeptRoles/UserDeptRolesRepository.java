package com.sungbok.community.repository.userDeptRoles;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.generated.tables.daos.UserDepartmentRolesDao;
import org.jooq.generated.tables.pojos.UserDepartmentRoles;
import org.jooq.generated.tables.records.UserDepartmentRolesRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static org.jooq.generated.Tables.USER_DEPARTMENT_ROLES;

@Repository
public class UserDeptRolesRepository {

    private final DSLContext dsl;
    private final UserDepartmentRolesDao dao;

    public UserDeptRolesRepository(DSLContext dsl, Configuration configuration) {
        this.dsl = dsl;
        this.dao = new UserDepartmentRolesDao(configuration);
    }

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
        record.setDepartmentName(userDepartmentRoles.getDepartmentName());
        record.setRoleId(userDepartmentRoles.getRoleId());
        record.setRoleName(userDepartmentRoles.getRoleName());
        record.store();

        return record.into(UserDepartmentRoles.class);

    }

    public List<UserDepartmentRoles> findAllByUserId(Long userId) {
        return dao.fetchByUserId(userId);
    }

}