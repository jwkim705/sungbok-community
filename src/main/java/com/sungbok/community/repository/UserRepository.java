package com.sungbok.community.repository;

import com.sungbok.community.dto.UserMemberDTO;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.generated.tables.daos.UsersDao;
import org.jooq.generated.tables.pojos.Users;
import org.jooq.generated.tables.records.MembershipsRecord;
import org.jooq.generated.tables.records.MembershipRolesRecord;
import org.jooq.generated.tables.records.OauthAccountsRecord;
import org.jooq.generated.tables.records.UsersRecord;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.sungbok.community.repository.util.JooqTenantConditionUtils.orgIdCondition;
import static org.jooq.generated.Tables.MEMBERSHIPS;
import static org.jooq.generated.Tables.MEMBERSHIP_ROLES;
import static org.jooq.generated.Tables.OAUTH_ACCOUNTS;
import static org.jooq.generated.Tables.ORGANIZATIONS;
import static org.jooq.generated.Tables.ROLES;
import static org.jooq.generated.Tables.USERS;

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
   * 인증 시점에 사용할 이메일로 사용자 조회 (TenantContext 불필요)
   *
   * ⚠️ 경고: 이 메서드는 인증(Authentication) 시점에만 사용해야 합니다.
   * - TenantContext가 설정되기 전에 호출되는 용도
   * - 승인된(APPROVED) 멤버십만 조회
   * - PRIMARY 멤버십 우선 선택 (다중 조직 사용자)
   *
   * 일반적인 비즈니스 로직에서는 fetchUserWithDetailsByEmail()을 사용하세요.
   *
   * @param email 사용자 이메일
   * @return 사용자 상세 정보 Optional (없거나 승인되지 않은 멤버십만 있으면 빈 Optional)
   */
  public Optional<UserMemberDTO> fetchUserByEmailForAuthentication(String email) {
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
              .where(USERS.EMAIL.eq(email))
              .and(USERS.IS_DELETED.eq(false))
              .and(MEMBERSHIPS.STATUS.eq("APPROVED"))
              .limit(1)
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
      return dslContext.update(USERS)
              .set(USERS.IS_DELETED, true)
              .set(USERS.MODIFIED_AT, LocalDateTime.now())
              .where(USERS.ID.eq(userId))
              .execute();
  }

  /**
   * OAuth2 사용자 조회 또는 생성 (OAuth 2.1 표준 + Guest JWT 지원)
   * jOOQ store() 메서드를 활용한 "find or create" 패턴
   *
   * ⚠️ 변경사항 (2025-12-15):
   * - OAuth 로그인 시 users 테이블만 생성 (플랫폼 레벨)
   * - memberships, oauth_accounts는 가입 요청 시점에 생성
   * - Guest JWT 발급 (orgId=null, roleIds=[])
   *
   * @param provider OAuth2 공급자 (google, kakao, naver)
   * @param providerId OAuth2 공급자의 사용자 ID
   * @param email 이메일
   * @param name 이름
   * @param picture 프로필 사진 URL
   * @return Guest 사용자 정보 (orgId=null, roleIds=[])
   */
  public UserMemberDTO findOrCreateOAuthUser(
      String provider,
      String providerId,
      String email,
      String name,
      String picture
  ) {
      // ⚠️ orgId 제거: OAuth 로그인은 플랫폼 레벨

      // 1. 이메일로 사용자 조회 (find)
      UsersRecord userRecord = dslContext
          .selectFrom(USERS)
          .where(USERS.EMAIL.eq(email))
          .fetchOne();

      if (userRecord == null) {
          // 신규 가입: 새 사용자 생성 (create)
          userRecord = dslContext.newRecord(USERS);
          userRecord.setEmail(email);
          userRecord.setPassword(null);  // OAuth 사용자는 비밀번호 없음
          userRecord.setIsDeleted(false);
          userRecord.setCreatedAt(LocalDateTime.now());
      }

      // 공통: modified_at 업데이트 (로그인마다 갱신)
      userRecord.setModifiedAt(LocalDateTime.now());
      userRecord.store();  // ✅ store() - PK 유무로 INSERT or UPDATE 자동 판단

      Long userId = userRecord.getId();

      // 2. Guest DTO 반환 (orgId=null, roleIds=[])
      // memberships, oauth_accounts, roles는 가입 요청 시점에 생성됨
      return new UserMemberDTO(
          null,              // orgId (Guest 사용자)
          userId,            // userId
          email,             // email
          name,              // name (OAuth에서 가져온 이름)
          null,              // password
          null,              // birthdate
          null,              // gender
          null,              // address
          null,              // phoneNumber
          picture,           // picture (프로필 사진)
          null,              // registeredByUserId
          null,              // appTypeId
          List.of()          // roleIds (빈 리스트)
      );
  }

  /**
   * 조직의 기본 역할 ID 조회 (3단계 Fallback)
   *
   * 우선순위:
   * 1. "member" 이름의 역할 조회
   * 2. level=1인 역할 조회 (가장 낮은 권한)
   * 3. 모두 실패 시 명확한 예외 발생
   *
   * @param orgId 조직 ID
   * @return 기본 역할 ID
   * @throws IllegalStateException 기본 역할을 찾을 수 없는 경우
   */
  private Long getDefaultRoleId(Long orgId) {
      // 1단계: "member" 이름으로 검색 (기존 로직)
      Long roleId = dslContext.select(ROLES.ID)
          .from(ROLES)
          .where(ROLES.ORG_ID.eq(orgId))
          .and(ROLES.NAME.eq("member"))
          .fetchOne(ROLES.ID);

      if (roleId != null) {
          return roleId;
      }

      // 2단계: Fallback - level=1인 역할 검색
      return rolesRepository.fetchByOrgIdAndLevel(orgId, 1)
          .map(role -> role.getId())
          .orElseThrow(() -> new IllegalStateException(
              String.format(
                  "No default role found for organization %d. " +
                  "Please create a 'member' role or a role with level=1.",
                  orgId
              )
          ));
  }

}