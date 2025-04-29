package com.sungbok.community.repository.users;

import com.sungbok.community.dto.DepartmentRoleInfo;
import com.sungbok.community.dto.UserMemberDTO;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.generated.tables.daos.UsersDao;
import org.jooq.generated.tables.pojos.*;
import org.jooq.generated.tables.records.UsersRecord;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static org.jooq.generated.Tables.MEMBERS;
import static org.jooq.generated.Tables.USERS;
import static org.jooq.generated.Tables.ROLES;
import static org.jooq.generated.Tables.DEPARTMENTS;
import static org.jooq.generated.Tables.USER_DEPARTMENT_ROLES;
import static org.jooq.impl.DSL.multiset;
import static org.jooq.impl.DSL.select;

@Repository
public class UserRepository {

  private final DSLContext dsl;
  private final UsersDao dao;

  public UserRepository(DSLContext dsl, Configuration configuration) {
    this.dsl = dsl;
    this.dao = new UsersDao(configuration);
  }

  public Optional<Users> findById(Long id) {
    return Optional.ofNullable(dao.findById(id));
  }

  public Optional<Users> findByEmail(String email) {
     return dsl.selectFrom(USERS)
               .where(USERS.EMAIL.eq(email))
               .fetchOptionalInto(Users.class);
  }

  public Users save(Users user) {
      UsersRecord record = dsl.newRecord(USERS, user);
      return dsl.insertInto(USERS)
                .set(record)
                .returning()
                .fetchOneInto(Users.class);
  }

  public void updateUsingStore(Users user) {
       if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User POJO with ID must be provided for update.");
       }

       UsersRecord usersRecord = dsl.fetchOptional(USERS, USERS.ID.eq(user.getId()))
           .orElseThrow(() -> new RuntimeException("User not found for update with ID: " + user.getId()));

       usersRecord.setEmail(user.getEmail());
       usersRecord.setPassword(user.getPassword()); // 서비스단에서 이미 처리된 비밀번호
       usersRecord.store();
  }

  public Optional<UserMemberDTO> findUserWithDetailsById(Long userId) {

      Field<List<DepartmentRoleInfo>> departmentRolesMultiset =
              multiset(
                      select(
                              DEPARTMENTS.ID.as("departmentId"),
                              DEPARTMENTS.NAME.as("departmentName"),
                              ROLES.ID.as("roleId"),
                              ROLES.NAME.as("roleName"),
                              USER_DEPARTMENT_ROLES.ASSIGNMENT_DATE
                      )
                              .from(USER_DEPARTMENT_ROLES)
                              .join(DEPARTMENTS).on(USER_DEPARTMENT_ROLES.DEPARTMENT_ID.eq(DEPARTMENTS.ID))
                              .join(ROLES).on(USER_DEPARTMENT_ROLES.ROLE_ID.eq(ROLES.ID))
                              .where(USER_DEPARTMENT_ROLES.USER_ID.eq(userId))
              )
                      .as("departmentRoles")
                      .convertFrom(r -> r.into(DepartmentRoleInfo.class));


      return dsl.select(
              DSL.row(USERS.fields()),
              DSL.row(MEMBERS.fields()),
              departmentRolesMultiset
              )
              .from(USERS)
              .join(MEMBERS).on(MEMBERS.USER_ID.eq(USERS.ID))
              .where(USERS.ID.eq(userId))
              .fetchOptionalInto(UserMemberDTO.class);
  }

    public Optional<UserMemberDTO> findUserWithMemberByEmail(String email) {

        Field<List<DepartmentRoleInfo>> departmentRolesMultiset =
            multiset(
                    select(
                            DEPARTMENTS.ID.as("departmentId"),
                            DEPARTMENTS.NAME.as("departmentName"),
                            ROLES.ID.as("roleId"),
                            ROLES.NAME.as("roleName"),
                            USER_DEPARTMENT_ROLES.ASSIGNMENT_DATE
                    )
                            .from(USER_DEPARTMENT_ROLES)
                            .join(DEPARTMENTS).on(USER_DEPARTMENT_ROLES.DEPARTMENT_ID.eq(DEPARTMENTS.ID))
                            .join(ROLES).on(USER_DEPARTMENT_ROLES.ROLE_ID.eq(ROLES.ID))
                            .where(USER_DEPARTMENT_ROLES.USER_ID.eq(
                                    select(USERS.ID)
                                    .from(USERS)
                                    .where(USERS.EMAIL.eq(email))
                            )
                            )
            )
                    .as("departmentRoles")
                    .convertFrom(r -> r.into(DepartmentRoleInfo.class));


        return dsl.select(
                        DSL.row(USERS.fields()),
                        DSL.row(MEMBERS.fields()),
                        departmentRolesMultiset
                )
                .from(USERS)
                .join(MEMBERS).on(USERS.ID.eq(MEMBERS.USER_ID))
                .where(USERS.EMAIL.eq(email))
                .fetchOptionalInto(UserMemberDTO.class);
    }

}