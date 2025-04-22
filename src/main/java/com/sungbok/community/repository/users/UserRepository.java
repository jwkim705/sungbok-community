package com.sungbok.community.repository.users;

import com.sungbok.community.dto.UpdateUser;
import com.sungbok.community.dto.UpdateUserWithMember;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.generated.tables.JUsers;
import org.jooq.generated.tables.daos.UsersDao;
import org.jooq.generated.tables.pojos.Users;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepository {

  private final DSLContext dsl;
  private final JUsers USERS = JUsers.USERS;
  private final UsersDao dao;

  public UserRepository(DSLContext dsl, Configuration configuration) {
    this.dsl = dsl;
    this.dao = new UsersDao(configuration);
  }

  public Optional<Users> findById(Long id) {
    return dao.fetchOptionalById(id);
  }

  public Optional<Users> findByEmail(String email) {
    return dao.fetchOptionalByEmail(email);
  }

  public int updateOauthLogin(UpdateUser updateUser) {
    return dsl.update(USERS)
            .set(USERS.EMAIL, updateUser.getEmail())
            .set(USERS.PASSWORD, updateUser.getPassword())
            .where(USERS.ID.eq(updateUser.getId()))
            .execute();
  }




}
