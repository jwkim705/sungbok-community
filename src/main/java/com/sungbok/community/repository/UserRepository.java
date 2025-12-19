package com.sungbok.community.repository;

import static com.sungbok.community.repository.util.JooqTenantConditionUtils.orgIdCondition;
import static org.jooq.generated.Tables.MEMBERSHIPS;
import static org.jooq.generated.Tables.MEMBERSHIP_ROLES;
import static org.jooq.generated.Tables.ORGANIZATIONS;
import static org.jooq.generated.Tables.ROLES;
import static org.jooq.generated.Tables.USERS;

import com.sungbok.community.dto.UserMemberDTO;
import java.time.LocalDateTime;
import java.util.Optional;
import org.jooq.Configuration;
import org.jooq.generated.enums.MembershipStatus;
import org.jooq.generated.enums.OrganizationStatus;
import org.jooq.DSLContext;
import org.jooq.generated.tables.daos.UsersDao;
import org.jooq.generated.tables.pojos.Users;
import org.jooq.generated.tables.records.UsersRecord;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

/**
 * 사용자 데이터 접근 Repository
 * 하이브리드 DAO + DSL 패턴 사용
 */
@Repository
public class UserRepository {

  private final DSLContext dslContext;
  private final UsersDao dao;
  private final RolesRepository rolesRepository;

  public UserRepository(DSLContext dslContext, Configuration configuration, RolesRepository rolesRepository) {
    this.dslContext = dslContext;
    this.dao = new UsersDao(configuration);
    this.rolesRepository = rolesRepository;
  }

  /**
   * ID로 사용자 조회 (플랫폼 레벨 - org_id 필터링 없음)
   *
   * @param id 사용자 ID
   * @return 사용자 Optional (없으면 빈 Optional)
   */
  public Optional<Users> fetchById(Long id) {
    return dslContext.selectFrom(USERS)
            .where(USERS.ID.eq(id))
            .and(USERS.IS_DELETED.eq(false))
            .fetchOptionalInto(Users.class);
  }

  /**
   * 이메일로 사용자 조회 (플랫폼 레벨 - org_id 필터링 없음)
   *
   * @param email 사용자 이메일
   * @return 사용자 Optional (없으면 빈 Optional)
   */
  public Optional<Users> fetchByEmail(String email) {
     return dslContext.selectFrom(USERS)
               .where(USERS.EMAIL.eq(email))
               .and(USERS.IS_DELETED.eq(false))
               .fetchOptionalInto(Users.class);
  }

  /**
   * RETURNING 절로 새 사용자를 삽입합니다.
   * Users는 플랫폼 레벨이므로 org_id 설정 불필요
   *
   * @param user 삽입할 사용자 엔티티
   * @return 생성된 ID와 타임스탬프가 포함된 삽입된 사용자
   */
  public Users insert(Users user) {
      dao.insert(user);  // DAO 패턴
      return user;
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

       UsersRecord usersRecord = dslContext.fetchOptional(USERS,
               USERS.ID.eq(user.getId()))
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
      return dslContext.select(
                      MEMBERSHIPS.ORG_ID,
                      USERS.ID,
                      USERS.EMAIL,
                      MEMBERSHIPS.NAME,
                      USERS.PASSWORD,
                      MEMBERSHIPS.BIRTHDATE,
                      MEMBERSHIPS.GENDER,
                      MEMBERSHIPS.ADDRESS,
                      MEMBERSHIPS.PHONE_NUMBER,
                      MEMBERSHIPS.PICTURE,
                      MEMBERSHIPS.REGISTERED_BY_USER_ID,
                      ORGANIZATIONS.APP_TYPE_ID,
                      DSL.multiset(
                              DSL.select(MEMBERSHIP_ROLES.ROLE_ID)
                                      .from(MEMBERSHIP_ROLES)
                                      .where(MEMBERSHIP_ROLES.ORG_ID.eq(MEMBERSHIPS.ORG_ID))
                                      .and(MEMBERSHIP_ROLES.MEMBERSHIP_ID.eq(MEMBERSHIPS.ID))
                                      .orderBy(MEMBERSHIP_ROLES.IS_PRIMARY.desc())
                      ).convertFrom(r -> r.into(Long.class)).as("roleIds")
              )
              .from(USERS)
              .join(MEMBERSHIPS).on(MEMBERSHIPS.USER_ID.eq(USERS.ID))
              .join(ORGANIZATIONS).on(ORGANIZATIONS.ORG_ID.eq(MEMBERSHIPS.ORG_ID))
              .where(orgIdCondition(MEMBERSHIPS.ORG_ID))
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
        return dslContext.select(
                        MEMBERSHIPS.ORG_ID,
                        USERS.ID,
                        USERS.EMAIL,
                        MEMBERSHIPS.NAME,
                        USERS.PASSWORD,
                        MEMBERSHIPS.BIRTHDATE,
                        MEMBERSHIPS.GENDER,
                        MEMBERSHIPS.ADDRESS,
                        MEMBERSHIPS.PHONE_NUMBER,
                        MEMBERSHIPS.PICTURE,
                        MEMBERSHIPS.REGISTERED_BY_USER_ID,
                        ORGANIZATIONS.APP_TYPE_ID,
                        DSL.multiset(
                                DSL.select(MEMBERSHIP_ROLES.ROLE_ID)
                                        .from(MEMBERSHIP_ROLES)
                                        .where(MEMBERSHIP_ROLES.ORG_ID.eq(MEMBERSHIPS.ORG_ID))
                                        .and(MEMBERSHIP_ROLES.MEMBERSHIP_ID.eq(MEMBERSHIPS.ID))
                                        .orderBy(MEMBERSHIP_ROLES.IS_PRIMARY.desc())
                        ).convertFrom(r -> r.into(Long.class)).as("roleIds")
                )
                .from(USERS)
                .join(MEMBERSHIPS).on(MEMBERSHIPS.USER_ID.eq(USERS.ID))
                .join(ORGANIZATIONS).on(ORGANIZATIONS.ORG_ID.eq(MEMBERSHIPS.ORG_ID))
                .where(orgIdCondition(MEMBERSHIPS.ORG_ID))
                .and(USERS.EMAIL.eq(email))
                .and(USERS.IS_DELETED.eq(false))
                .fetchOptionalInto(UserMemberDTO.class);
  }

  /**
   * is_deleted 플래그를 설정하여 사용자를 소프트 삭제합니다. DSLContext.update() 패턴 사용 (app_id로 격리)
   *
   * @param userId 소프트 삭제할 사용자 ID
   */
  public void softDelete(Long userId) {
      dslContext.update(USERS)
          .set(USERS.IS_DELETED, true)
          .set(USERS.MODIFIED_AT, LocalDateTime.now())
          .where(USERS.ID.eq(userId))
          .execute();
  }

  /**
   * OAuth2 사용자 Upsert (조회 없이 한 번에 처리)
   *
   * PostgreSQL ON CONFLICT 활용:
   * - 신규 사용자: INSERT
   * - 기존 사용자: modified_at 업데이트
   *
   * @param email 이메일
   * @param name OAuth에서 가져온 이름 (미사용, 향후 확장용)
   * @param picture 프로필 사진 URL (미사용, 향후 확장용)
   * @return Users POJO (플랫폼 레벨 사용자 정보만)
   */
  public Users upsertOAuthUser(String email, String name, String picture) {
      LocalDateTime now = LocalDateTime.now();

      return dslContext.insertInto(USERS)
          .columns(USERS.EMAIL, USERS.PASSWORD, USERS.IS_DELETED,
                   USERS.CREATED_AT, USERS.MODIFIED_AT)
          .values(email, null, false, now, now)
          .onConflict(USERS.EMAIL)  // PostgreSQL UNIQUE 제약 조건
          .doUpdate()
          .set(USERS.MODIFIED_AT, now)  // 로그인 시각 갱신
          .returning()
          .fetchOneInto(Users.class);
  }

  /**
   * 사용자의 특정 조직 Full 정보 조회 (APPROVED 멤버십 필수)
   *
   * OAuth 로그인 시 X-Org-Id 헤더로 지정된 조직의 멤버십 조회
   * APPROVED 멤버십이 있으면 orgId, roleIds 포함된 Full DTO 반환
   *
   * @param userId 사용자 ID
   * @param orgId 조직 ID
   * @return Full UserMemberDTO (orgId, roleIds 포함), 없으면 Empty
   */
  public Optional<UserMemberDTO> fetchUserWithMembershipByOrgId(Long userId, Long orgId) {
      return dslContext
          .select(
              USERS.ID,
              USERS.EMAIL,
              USERS.PASSWORD,
              MEMBERSHIPS.ORG_ID,
              MEMBERSHIPS.NAME,
              MEMBERSHIPS.BIRTHDATE,
              MEMBERSHIPS.GENDER,
              MEMBERSHIPS.ADDRESS,
              MEMBERSHIPS.PHONE_NUMBER,
              MEMBERSHIPS.PICTURE,
              MEMBERSHIPS.REGISTERED_BY_USER_ID,
              ORGANIZATIONS.APP_TYPE_ID,
              DSL.multiset(
                  DSL.select(MEMBERSHIP_ROLES.ROLE_ID)
                      .from(MEMBERSHIP_ROLES)
                      .where(MEMBERSHIP_ROLES.MEMBERSHIP_ID.eq(MEMBERSHIPS.ID))
              ).convertFrom(r -> r.into(Long.class)).as("roleIds")
          )
          .from(USERS)
          .join(MEMBERSHIPS).on(MEMBERSHIPS.USER_ID.eq(USERS.ID))
          .join(ORGANIZATIONS).on(ORGANIZATIONS.ORG_ID.eq(MEMBERSHIPS.ORG_ID))
          .where(USERS.ID.eq(userId))
          .and(MEMBERSHIPS.ORG_ID.eq(orgId))
          .and(MEMBERSHIPS.STATUS.eq(MembershipStatus.APPROVED))
          .and(USERS.IS_DELETED.eq(false))
          .fetchOptionalInto(UserMemberDTO.class);
  }

  /**
   * 로그인용 사용자 정보 조회 (멤버십 여부 관계없이)
   *
   * 한 번의 쿼리로 Users + Organizations + Memberships(optional) 조회:
   * - 멤버십 있으면: orgId, appTypeId, roleIds 포함 (회원)
   * - 멤버십 없으면: orgId, appTypeId 포함, roleIds=[] (비회원, 가입 요청 가능)
   *
   * @param userId 사용자 ID
   * @param orgId 조직 ID
   * @return UserMemberDTO (멤버십 없어도 반환)
   */
  public UserMemberDTO fetchUserForLogin(Long userId, Long orgId) {
      return dslContext.select(
              ORGANIZATIONS.ORG_ID,
              USERS.ID.as("userId"),
              USERS.EMAIL,
              DSL.coalesce(MEMBERSHIPS.NAME, USERS.EMAIL).as("name"),  // 멤버십 없으면 email 사용
              USERS.PASSWORD,
              MEMBERSHIPS.BIRTHDATE,
              MEMBERSHIPS.GENDER,
              MEMBERSHIPS.ADDRESS,
              MEMBERSHIPS.PHONE_NUMBER,
              MEMBERSHIPS.PICTURE,
              MEMBERSHIPS.REGISTERED_BY_USER_ID,
              ORGANIZATIONS.APP_TYPE_ID,
              DSL.multiset(
                  DSL.select(MEMBERSHIP_ROLES.ROLE_ID)
                      .from(MEMBERSHIP_ROLES)
                      .where(MEMBERSHIP_ROLES.MEMBERSHIP_ID.eq(MEMBERSHIPS.ID))
                      .and(MEMBERSHIP_ROLES.ORG_ID.eq(MEMBERSHIPS.ORG_ID))
              ).convertFrom(r -> r.into(Long.class)).as("roleIds")
          )
          .from(USERS)
          .innerJoin(ORGANIZATIONS).on(ORGANIZATIONS.ORG_ID.eq(orgId))  // orgId 사전 필터링 (CROSS JOIN 제거)
          .leftJoin(MEMBERSHIPS)     // 멤버십은 optional
              .on(MEMBERSHIPS.USER_ID.eq(USERS.ID))
              .and(MEMBERSHIPS.ORG_ID.eq(ORGANIZATIONS.ORG_ID))
              .and(MEMBERSHIPS.STATUS.eq(MembershipStatus.APPROVED))
          .where(USERS.ID.eq(userId))
          .and(USERS.IS_DELETED.eq(false))
          .fetchOneInto(UserMemberDTO.class);
  }

}