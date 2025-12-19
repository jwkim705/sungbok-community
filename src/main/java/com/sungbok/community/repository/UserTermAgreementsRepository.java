package com.sungbok.community.repository;

import com.sungbok.community.dto.UserTermAgreementDTO;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.generated.tables.daos.UserTermAgreementsDao;
import org.jooq.generated.tables.pojos.UserTermAgreements;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static org.jooq.generated.Tables.TERMS;
import static org.jooq.generated.Tables.USER_TERM_AGREEMENTS;

/**
 * 사용자 약관 동의 이력 데이터 접근 Repository
 * 법적 증거로 사용되므로 변경 불가 (INSERT only)
 * 하이브리드 DAO + DSL 패턴 사용
 *
 * 특징: Platform-level 테이블 (org_id 없음), 모든 사용자 동의 이력 추적
 */
@Repository
public class UserTermAgreementsRepository {

    private final DSLContext dslContext;
    private final UserTermAgreementsDao dao;

    public UserTermAgreementsRepository(DSLContext dslContext, Configuration configuration) {
        this.dslContext = dslContext;
        this.dao = new UserTermAgreementsDao(configuration);
    }

    /**
     * 사용자 약관 동의 기록을 삽입합니다.
     * 법적 증거로 사용되므로 변경/삭제 불가
     *
     * @param agreement 삽입할 동의 이력 엔티티
     * @return 삽입된 동의 이력 (모든 필드 포함)
     */
    public UserTermAgreements insert(UserTermAgreements agreement) {
        // 현재 시간 설정 (agreed_at)
        if (agreement.getAgreedAt() == null) {
            agreement.setAgreedAt(LocalDateTime.now());
        }

        dao.insert(agreement);  // DAO 패턴
        return agreement;
    }

    /**
     * 여러 약관 동의 기록을 일괄 삽입합니다.
     * 회원가입 시 여러 약관 동의를 한 번에 처리
     *
     * @param agreements 삽입할 동의 이력 리스트
     */
    public void batchInsert(List<UserTermAgreements> agreements) {
        LocalDateTime now = LocalDateTime.now();
        for (UserTermAgreements agreement : agreements) {
            if (agreement.getAgreedAt() == null) {
                agreement.setAgreedAt(now);
            }
        }

        dao.insert(agreements);  // Batch INSERT
    }

    /**
     * 사용자가 특정 약관에 동의했는지 확인합니다.
     *
     * @param userId 사용자 ID
     * @param termId 약관 ID
     * @return 동의 여부
     */
    public boolean hasAgreed(Long userId, Long termId) {
        return dslContext.fetchExists(
                USER_TERM_AGREEMENTS,
                USER_TERM_AGREEMENTS.USER_ID.eq(userId)
                        .and(USER_TERM_AGREEMENTS.TERM_ID.eq(termId))
        );
    }

    /**
     * 사용자의 모든 약관 동의 이력 조회
     *
     * @param userId 사용자 ID
     * @return 약관 동의 이력 리스트 (최신순)
     */
    public List<UserTermAgreements> fetchByUserId(Long userId) {
        return dslContext.selectFrom(USER_TERM_AGREEMENTS)
                .where(USER_TERM_AGREEMENTS.USER_ID.eq(userId))
                .orderBy(USER_TERM_AGREEMENTS.AGREED_AT.desc())
                .fetchInto(UserTermAgreements.class);
    }

    /**
     * 특정 약관에 동의한 모든 사용자 조회
     * 관리자용 (통계/분석)
     *
     * @param termId 약관 ID
     * @return 약관 동의 이력 리스트 (최신순)
     */
    public List<UserTermAgreements> fetchByTermId(Long termId) {
        return dslContext.selectFrom(USER_TERM_AGREEMENTS)
                .where(USER_TERM_AGREEMENTS.TERM_ID.eq(termId))
                .orderBy(USER_TERM_AGREEMENTS.AGREED_AT.desc())
                .fetchInto(UserTermAgreements.class);
    }

    /**
     * 사용자의 특정 약관 동의 기록 조회
     *
     * @param userId 사용자 ID
     * @param termId 약관 ID
     * @return 약관 동의 기록 (없으면 null)
     */
    public UserTermAgreements fetchByUserIdAndTermId(Long userId, Long termId) {
        return dslContext.selectFrom(USER_TERM_AGREEMENTS)
                .where(USER_TERM_AGREEMENTS.USER_ID.eq(userId))
                .and(USER_TERM_AGREEMENTS.TERM_ID.eq(termId))
                .fetchOneInto(UserTermAgreements.class);
    }

    /**
     * 사용자가 현재 유효한 모든 필수 약관에 동의했는지 확인합니다.
     * 회원가입 시 검증용
     *
     * @param userId 사용자 ID
     * @param requiredTermIds 필수 약관 ID 리스트
     * @return 모든 필수 약관 동의 여부
     */
    public boolean hasAgreedToAllRequiredTerms(Long userId, List<Long> requiredTermIds) {
        if (requiredTermIds == null || requiredTermIds.isEmpty()) {
            return true;
        }

        int agreedCount = dslContext.selectCount()
                .from(USER_TERM_AGREEMENTS)
                .where(USER_TERM_AGREEMENTS.USER_ID.eq(userId))
                .and(USER_TERM_AGREEMENTS.TERM_ID.in(requiredTermIds))
                .fetchOneInto(Integer.class);

        return agreedCount == requiredTermIds.size();
    }

    /**
     * 사용자 약관 동의 이력 조회 (약관 정보 포함 JOIN)
     * terms 테이블과 JOIN하여 약관 제목과 버전 정보 포함
     *
     * @param userId 사용자 ID
     * @return UserTermAgreementDTO 리스트 (termTitle, termVersion 포함)
     */
    public List<UserTermAgreementDTO> fetchByUserIdWithTermInfo(Long userId) {
        return dslContext.select(
                        USER_TERM_AGREEMENTS.ID,
                        USER_TERM_AGREEMENTS.USER_ID,
                        USER_TERM_AGREEMENTS.TERM_ID,
                        USER_TERM_AGREEMENTS.AGREED_AT,
                        USER_TERM_AGREEMENTS.IP_ADDRESS,
                        TERMS.TITLE.as("termTitle"),
                        TERMS.VERSION.as("termVersion")
                )
                .from(USER_TERM_AGREEMENTS)
                .join(TERMS).on(USER_TERM_AGREEMENTS.TERM_ID.eq(TERMS.ID))
                .where(USER_TERM_AGREEMENTS.USER_ID.eq(userId))
                .orderBy(USER_TERM_AGREEMENTS.AGREED_AT.desc())
                .fetch(record -> UserTermAgreementDTO.builder()
                        .id(record.get(USER_TERM_AGREEMENTS.ID))
                        .userId(record.get(USER_TERM_AGREEMENTS.USER_ID))
                        .termId(record.get(USER_TERM_AGREEMENTS.TERM_ID))
                        .agreedAt(record.get(USER_TERM_AGREEMENTS.AGREED_AT))
                        .ipAddress(record.get(USER_TERM_AGREEMENTS.IP_ADDRESS) != null
                                ? record.get(USER_TERM_AGREEMENTS.IP_ADDRESS).toString()
                                : null)
                        .termTitle(record.get("termTitle", String.class))
                        .termVersion(record.get("termVersion", String.class))
                        .build()
                );
    }
}
