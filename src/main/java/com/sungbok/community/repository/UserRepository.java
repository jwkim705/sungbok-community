package com.sungbok.community.repository;

import com.sungbok.community.dto.AddUserRequestDTO;
import com.sungbok.community.dto.UserMemberDTO;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.generated.tables.daos.UsersDao;
import org.jooq.generated.tables.pojos.Users;
import org.jooq.generated.tables.records.UsersRecord;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.jooq.generated.Tables.MEMBERS;
import static org.jooq.generated.Tables.USERS;

@Repository
public class UserRepository {

  private final DSLContext dsl;
  private final UsersDao dao;
  private final BCryptPasswordEncoder passwordEncoder;

  public UserRepository(DSLContext dsl, Configuration configuration, BCryptPasswordEncoder passwordEncoder) {
    this.dsl = dsl;
    this.dao = new UsersDao(configuration);
      this.passwordEncoder = passwordEncoder;
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

  public Users save(AddUserRequestDTO dto) {

      Users user = new Users()
              .setEmail(dto.getEmail())
              .setPassword(passwordEncoder.encode(dto.getPassword()))
              .setIsDeleted(false)
              .setCreatedAt(LocalDateTime.now())
              .setModifiedAt(LocalDateTime.now());

      return this.save(user);
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
                      MEMBERS.ROLE                    // 11. role
              )
              .from(USERS)
              .join(MEMBERS).on(MEMBERS.USER_ID.eq(USERS.ID))
              .where(USERS.ID.eq(userId))
              .fetchOptionalInto(UserMemberDTO.class);
    }

    public Optional<UserMemberDTO> findUserWithDetailsByEmailOptional(String email) {
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
                        MEMBERS.ROLE                    // 11. role
                )
                .from(USERS)
                .join(MEMBERS).on(MEMBERS.USER_ID.eq(USERS.ID))
                .where(USERS.EMAIL.eq(email))
                .fetchOptionalInto(UserMemberDTO.class);
    }

    public UserMemberDTO findUserWithDetailsByEmail(String email) {
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
                        MEMBERS.ROLE                    // 11. role
                )
                .from(USERS)
                .join(MEMBERS).on(MEMBERS.USER_ID.eq(USERS.ID))
                .where(USERS.EMAIL.eq(email))
                .fetchOneInto(UserMemberDTO.class);
    }

    public void deleteUser(Long userId) {
      dao.deleteById(userId);
    }

}