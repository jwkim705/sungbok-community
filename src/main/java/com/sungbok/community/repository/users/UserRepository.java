package com.sungbok.community.repository.users;

import com.sungbok.community.dto.UserMemberDTO;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.generated.tables.daos.UsersDao;
import org.jooq.generated.tables.pojos.Members;
import org.jooq.generated.tables.pojos.Users;
import org.jooq.generated.tables.records.UsersRecord;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static org.jooq.generated.Tables.MEMBERS;
import static org.jooq.generated.Tables.USERS;

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

  /**
   * 사용자 ID를 기반으로 Users와 Members 정보를 JOIN하여 UserMemberDTO를 조회합니다.
   * @param userId 조회할 사용자의 ID
   * @return UserMemberDTO가 포함된 Optional 객체. 사용자가 없거나 멤버 정보가 없으면 비어 있을 수 있음.
   */
  public Optional<UserMemberDTO> findUserWithMemberById(Long userId) {
      Record result = dsl.select(USERS.asterisk(), MEMBERS.asterisk())
          .from(USERS)
          .join(MEMBERS).on(USERS.ID.eq(MEMBERS.USER_ID))
          .where(USERS.ID.eq(userId))
          .fetchOne(); // fetchOptional() 사용 고려 가능

      if (result == null) {
          return Optional.empty(); // 결과가 없으면 빈 Optional 반환
      }

      Users finalUser = result.into(Users.class);
      Members finalMember = result.into(Members.class);

      return Optional.of(UserMemberDTO.of(finalUser, finalMember));
  }

    public Optional<UserMemberDTO> findUserWithMemberByEmail(String email) {
        Optional<Record> result = dsl.select(USERS.asterisk(), MEMBERS.asterisk())
                .from(USERS)
                .join(MEMBERS).on(USERS.ID.eq(MEMBERS.USER_ID))
                .where(USERS.EMAIL.eq(email))
                .fetchOptional(); // fetchOptional() 사용 고려 가능

        if (result.isEmpty()) {
            return Optional.empty(); // 결과가 없으면 빈 Optional 반환
        }

        Users finalUser = result.get().into(Users.class);
        Members finalMember = result.get().into(Members.class);

        return Optional.of(UserMemberDTO.of(finalUser, finalMember));
    }
}