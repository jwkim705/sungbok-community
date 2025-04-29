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
                            USER_DEPARTMENT_ROLES.DEPARTMENT_ID.as("departmentId"),
                            USER_DEPARTMENT_ROLES.DEPARTMENT_NAME,
                            USER_DEPARTMENT_ROLES.ROLE_ID.as("roleId"),
                            USER_DEPARTMENT_ROLES.ROLE_NAME,
                            USER_DEPARTMENT_ROLES.ASSIGNMENT_DATE
                    )
                            .from(USER_DEPARTMENT_ROLES)
                            .where(USER_DEPARTMENT_ROLES.USER_ID.eq(userId))
            )
                    .as("departmentRoles")
                    .convertFrom(r -> r.into(DepartmentRoleInfo.class));


    return dsl.select(
                    USERS.ID,                       // 1. userId
                    USERS.EMAIL,                    // 2. email
                    MEMBERS.NAME,                   // 3. name
                    USERS.PASSWORD,                 // 4. password
                    MEMBERS.BIRTHDATE,              // 5. birthdate
                    MEMBERS.GENDER,                 // 6. gender
                    MEMBERS.ADDRESS,                // 7. address
                    MEMBERS.PHONE_NUMBER,           // 8. phoneNumber
                    MEMBERS.PICTURE,                // 9. picture
                    MEMBERS.REGISTERED_BY_USER_ID,  // 10. registeredByUserId
                    departmentRolesMultiset         // 11. departmentRoles
            )
            .from(USERS)
            .join(MEMBERS).on(MEMBERS.USER_ID.eq(USERS.ID))
            .where(USERS.ID.eq(userId))
            .fetchOptional() // Fetch a single user record (Optional)
            .map(record -> {
                List<DepartmentRoleInfo> rolesList =
                        record.get("departmentRoles", List.class);
                return new UserMemberDTO(
                        record.get(USERS.ID),                     // 1
                        record.get(USERS.EMAIL),                  // 2
                        record.get(MEMBERS.NAME),                 // 3
                        record.get(USERS.PASSWORD),               // 4
                        record.get(MEMBERS.BIRTHDATE),            // 5
                        record.get(MEMBERS.GENDER),               // 6
                        record.get(MEMBERS.ADDRESS),              // 7
                        record.get(MEMBERS.PHONE_NUMBER),         // 8
                        record.get(MEMBERS.PICTURE),              // 9
                        record.get(MEMBERS.REGISTERED_BY_USER_ID),// 10
                        rolesList                                 // 11
                );
            });
    }

    public Optional<UserMemberDTO> findUserWithDetailsByEmail(String email) {

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
                        USERS.ID,                       // 1. userId
                        USERS.EMAIL,                    // 2. email
                        MEMBERS.NAME,                   // 3. name
                        USERS.PASSWORD,                 // 4. password
                        MEMBERS.BIRTHDATE,              // 5. birthdate
                        MEMBERS.GENDER,                 // 6. gender
                        MEMBERS.ADDRESS,                // 7. address
                        MEMBERS.PHONE_NUMBER,           // 8. phoneNumber
                        MEMBERS.PICTURE,                // 9. picture
                        MEMBERS.REGISTERED_BY_USER_ID,  // 10. registeredByUserId
                        departmentRolesMultiset         // 11. departmentRoles
                )
                .from(USERS)
                .join(MEMBERS).on(MEMBERS.USER_ID.eq(USERS.ID))
                .where(USERS.EMAIL.eq(email))
                .fetchOptional()
                .map(record -> {
                    List<DepartmentRoleInfo> rolesList =
                            record.get("departmentRoles", List.class);
                    return new UserMemberDTO(
                            record.get(USERS.ID),                     // 1
                            record.get(USERS.EMAIL),                  // 2
                            record.get(MEMBERS.NAME),                 // 3
                            record.get(USERS.PASSWORD),               // 4
                            record.get(MEMBERS.BIRTHDATE),            // 5
                            record.get(MEMBERS.GENDER),               // 6
                            record.get(MEMBERS.ADDRESS),              // 7
                            record.get(MEMBERS.PHONE_NUMBER),         // 8
                            record.get(MEMBERS.PICTURE),              // 9
                            record.get(MEMBERS.REGISTERED_BY_USER_ID),// 10
                            rolesList                                 // 11
                    );
                });
    }

}