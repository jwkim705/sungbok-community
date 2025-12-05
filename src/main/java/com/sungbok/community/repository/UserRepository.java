package com.sungbok.community.repository;

import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.security.TenantContext;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.generated.tables.daos.UsersDao;
import org.jooq.generated.tables.pojos.Users;
import org.jooq.generated.tables.records.UsersRecord;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.sungbok.community.repository.util.JooqTenantConditionUtils.appIdCondition;
import static org.jooq.generated.Tables.MEMBERS;
import static org.jooq.generated.Tables.USERS;

/**
 * 사용자 데이터 접근 Repository
 * 하이브리드 DAO + DSL 패턴 사용
 */
@Repository
public class UserRepository {

  private final DSLContext dsl;
  private final UsersDao dao;

  public UserRepository(DSLContext dsl, Configuration configuration) {
    this.dsl = dsl;
    this.dao = new UsersDao(configuration);
  }

  /**
   * ID로 사용자 조회 (app_id 자동 필터링)
   *
   * @param id 사용자 ID
   * @return 사용자 Optional (없으면 빈 Optional)
   */
  public Optional<Users> fetchById(Long id) {
    return dsl.selectFrom(USERS)
            .where(appIdCondition(USERS.APP_ID))
            .and(USERS.ID.eq(id))
            .and(USERS.IS_DELETED.eq(false))
            .fetchOptionalInto(Users.class);
  }

  /**
   * 이메일로 사용자 조회 (app_id 자동 필터링)
   *
   * @param email 사용자 이메일
   * @return 사용자 Optional (없으면 빈 Optional)
   */
  public Optional<Users> fetchByEmail(String email) {
     return dsl.selectFrom(USERS)
               .where(appIdCondition(USERS.APP_ID))
               .and(USERS.EMAIL.eq(email))
               .and(USERS.IS_DELETED.eq(false))
               .fetchOptionalInto(Users.class);
  }

  /**
   * RETURNING 절로 새 사용자를 삽입합니다.
   * app_id는 TenantContext에서 자동 설정
   *
   * @param user 삽입할 사용자 엔티티
   * @return 생성된 ID와 타임스탬프가 포함된 삽입된 사용자
   */
  public Users insert(Users user) {
      // TenantContext에서 app_id 가져오기
      Long appId = TenantContext.getRequiredAppId();
      user.setAppId(appId);  // 강제로 현재 테넌트 설정

      UsersRecord record = dsl.newRecord(USERS, user);
      return dsl.insertInto(USERS)
                .set(record)
                .returning()
                .fetchOneInto(Users.class);
  }

  /**
   * Record.store() 패턴으로 사용자를 업데이트합니다.
   * app_id로 격리
   *
   * @param user 업데이트할 사용자 엔티티
   */
  public void update(Users user) {
       if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User with ID must be provided for update.");
       }

       UsersRecord usersRecord = dsl.fetchOptional(USERS,
               appIdCondition(USERS.APP_ID)
                       .and(USERS.ID.eq(user.getId())))
           .orElseThrow(() -> new RuntimeException("User not found with ID: " + user.getId()));

       usersRecord.setEmail(user.getEmail());
       usersRecord.setPassword(user.getPassword());
       usersRecord.setModifiedAt(LocalDateTime.now());
       usersRecord.store();
  }

  /**
   * 사용자 ID로 사용자 상세 정보를 조회합니다.
   * 명시적 JOIN 사용 (app_id 필터링)
   *
   * @param userId 사용자 ID
   * @return 사용자 상세 정보 Optional (없으면 빈 Optional)
   */
  public Optional<UserMemberDTO> fetchUserWithDetailsById(Long userId) {
      return dsl.select(
                      USERS.APP_ID,
                      USERS.ID,
                      USERS.EMAIL,
                      MEMBERS.NAME,
                      USERS.PASSWORD,
                      MEMBERS.BIRTHDATE,
                      MEMBERS.GENDER,
                      MEMBERS.ADDRESS,
                      MEMBERS.PHONE_NUMBER,
                      MEMBERS.PICTURE,
                      MEMBERS.REGISTERED_BY_USER_ID,
                      MEMBERS.ROLE
              )
              .from(USERS)
              .join(MEMBERS).on(
                      MEMBERS.APP_ID.eq(USERS.APP_ID)
                              .and(MEMBERS.USER_ID.eq(USERS.ID))
              )
              .where(appIdCondition(USERS.APP_ID))
              .and(USERS.ID.eq(userId))
              .and(USERS.IS_DELETED.eq(false))
              .fetchOptionalInto(UserMemberDTO.class);
  }

  /**
   * 이메일로 사용자 상세 정보를 조회합니다.
   * 명시적 JOIN 사용 (app_id 필터링)
   *
   * @param email 사용자 이메일
   * @return 사용자 상세 정보 Optional (없으면 빈 Optional)
   */
  public Optional<UserMemberDTO> fetchUserWithDetailsByEmail(String email) {
        return dsl.select(
                        USERS.APP_ID,
                        USERS.ID,
                        USERS.EMAIL,
                        MEMBERS.NAME,
                        USERS.PASSWORD,
                        MEMBERS.BIRTHDATE,
                        MEMBERS.GENDER,
                        MEMBERS.ADDRESS,
                        MEMBERS.PHONE_NUMBER,
                        MEMBERS.PICTURE,
                        MEMBERS.REGISTERED_BY_USER_ID,
                        MEMBERS.ROLE
                )
                .from(USERS)
                .join(MEMBERS).on(
                        MEMBERS.APP_ID.eq(USERS.APP_ID)
                                .and(MEMBERS.USER_ID.eq(USERS.ID))
                )
                .where(appIdCondition(USERS.APP_ID))
                .and(USERS.EMAIL.eq(email))
                .and(USERS.IS_DELETED.eq(false))
                .fetchOptionalInto(UserMemberDTO.class);
  }

  /**
   * is_deleted 플래그를 설정하여 사용자를 소프트 삭제합니다.
   * DSLContext.update() 패턴 사용 (app_id로 격리)
   *
   * @param userId 소프트 삭제할 사용자 ID
   * @return 영향받은 행 수
   */
  public int softDelete(Long userId) {
      return dsl.update(USERS)
              .set(USERS.IS_DELETED, true)
              .set(USERS.MODIFIED_AT, LocalDateTime.now())
              .where(appIdCondition(USERS.APP_ID))
              .and(USERS.ID.eq(userId))
              .execute();
  }

}