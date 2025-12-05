package com.sungbok.community.repository;

import com.sungbok.community.security.TenantContext;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.generated.tables.daos.MembersDao;
import org.jooq.generated.tables.pojos.Members;
import org.jooq.generated.tables.records.MembersRecord;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.sungbok.community.repository.util.JooqTenantConditionUtils.appIdCondition;
import static org.jooq.generated.Tables.MEMBERS;

/**
 * 회원 데이터 접근 Repository
 * 하이브리드 DAO + DSL 패턴 사용
 */
@Repository
public class MembersRepository {

    private final DSLContext dsl;
    private final MembersDao dao;

    public MembersRepository(DSLContext dsl, Configuration configuration) {
        this.dsl = dsl;
        this.dao = new MembersDao(configuration);
    }

    /**
     * ID로 회원 조회 (app_id 자동 필터링)
     *
     * @param id 회원 ID
     * @return 회원 Optional (없으면 빈 Optional)
     */
    public Optional<Members> fetchById(Long id) {
        return dsl.selectFrom(MEMBERS)
                .where(appIdCondition(MEMBERS.APP_ID))
                .and(MEMBERS.ID.eq(id))
                .fetchOptionalInto(Members.class);
    }

    /**
     * 사용자 ID로 회원 조회 (app_id 자동 필터링)
     *
     * @param userId 사용자 ID
     * @return 회원 Optional (없으면 빈 Optional)
     */
    public Optional<Members> fetchByUserId(Long userId) {
         return dsl.selectFrom(MEMBERS)
                  .where(appIdCondition(MEMBERS.APP_ID))
                  .and(MEMBERS.USER_ID.eq(userId))
                  .fetchOptionalInto(Members.class);
    }

    /**
     * RETURNING 절로 새 회원을 삽입합니다.
     * app_id는 TenantContext에서 자동 설정
     *
     * @param member 삽입할 회원 엔티티
     * @return 모든 필드가 포함된 삽입된 회원
     */
    public Members insert(Members member) {
        // TenantContext에서 app_id 가져오기
        Long appId = TenantContext.getRequiredAppId();
        member.setAppId(appId);  // 강제로 현재 테넌트 설정

        MembersRecord record = dsl.newRecord(MEMBERS, member);
        return dsl.insertInto(MEMBERS)
                .set(record)
                .returning()
                .fetchOneInto(Members.class);
    }

    /**
     * Record.store() 패턴으로 회원을 업데이트합니다.
     * app_id로 격리
     *
     * @param member 업데이트할 회원 엔티티
     */
    public void update(Members member) {
       if (member == null || member.getId() == null) {
            throw new IllegalArgumentException("Member with ID must be provided for update.");
       }

       MembersRecord membersRecord = dsl.fetchOptional(MEMBERS,
               appIdCondition(MEMBERS.APP_ID)
                       .and(MEMBERS.ID.eq(member.getId())))
           .orElseThrow(() -> new RuntimeException("Member not found with ID: " + member.getId()));

       membersRecord.setName(member.getName());
       membersRecord.setBirthdate(member.getBirthdate());
       membersRecord.setGender(member.getGender());
       membersRecord.setAddress(member.getAddress());
       membersRecord.setPhoneNumber(member.getPhoneNumber());
       membersRecord.setPicture(member.getPicture());
       membersRecord.setModifiedAt(LocalDateTime.now());
       membersRecord.store();
   }

}