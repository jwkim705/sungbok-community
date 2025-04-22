package com.sungbok.community.repository;

import static org.jooq.generated.tables.JUsers.USERS;

import com.sungbok.community.security.model.OAuthAttributes;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.generated.tables.JUsers;
import org.jooq.generated.tables.daos.UsersDao;
import org.jooq.generated.tables.pojos.Users;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

  private final DSLContext dsl;
  private final JUsers JUSERS = USERS;
  private final UsersDao dao;

  public UserRepository(DSLContext dsl, Configuration configuration) {
    this.dsl = dsl;
    this.dao = new UsersDao(configuration);
  }

  public Users findById(Long id) {
    return dao.fetchOneById(id);
  }

  public Users findByEmail(String email) {
    return dao.fetchOneByEmail(email);
  }

  public void updateOauthLogin(OAuthAttributes attributes) {
    dsl.update(USERS)
        .set(USERS.NAME, attributes.getName())
        .where(USERS.EMAIL.eq(attributes.getEmail()))
        .execute();
  }




}
