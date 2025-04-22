package com.sungbok.community.repository.member;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.generated.tables.JMembers;
import org.jooq.generated.tables.daos.MembersDao;
import org.jooq.generated.tables.pojos.Members;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MembersRepository {

  private final DSLContext dsl;
  private final JMembers MEMBERS = JMembers.MEMBERS;
  private final MembersDao dao;

  public MembersRepository(DSLContext dsl, Configuration configuration) {
    this.dsl = dsl;
    this.dao = new MembersDao(configuration);
  }

  public Optional<Members> findByUserId(Long userId) {
    return dao.fetchOptionalByUserId(userId);
  }

}
