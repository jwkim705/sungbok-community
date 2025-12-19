package com.sungbok.community.repository;

import com.sungbok.community.security.TenantContext;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.generated.enums.TermType;
import org.jooq.generated.tables.daos.TermsDao;
import org.jooq.generated.tables.pojos.Terms;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.sungbok.community.repository.util.JooqTenantConditionUtils.orgIdCondition;
import static org.jooq.generated.Tables.TERMS;

/**
 * 약관 데이터 접근 Repository
 * 약관 버전 관리 (이용약관, 개인정보처리방침, 마케팅동의)
 * 하이브리드 DAO + DSL 패턴 사용
 *
 * 특징: org_id가 NULL이면 플랫폼 전체 약관 (모든 조직 공통)
 */
@Repository
public class TermsRepository {

    private final DSLContext dslContext;
    private final TermsDao dao;

    public TermsRepository(DSLContext dslContext, Configuration configuration) {
        this.dslContext = dslContext;
        this.dao = new TermsDao(configuration);
    }

    /**
     * 현재 유효한 약관 조회 (is_current = true)
     * org_id가 NULL인 플랫폼 전체 약관과 조직별 약관 모두 조회
     *
     * @param orgId 조직 ID (NULL이면 플랫폼 전체 약관만 조회)
     * @return 현재 유효한 약관 리스트
     */
    public List<Terms> fetchCurrentTerms(Long orgId) {
        if (orgId == null) {
            // 플랫폼 전체 약관만 조회
            return dslContext.selectFrom(TERMS)
                    .where(TERMS.ORG_ID.isNull())
                    .and(TERMS.IS_CURRENT.eq(true))
                    .fetchInto(Terms.class);
        } else {
            // 조직별 약관 + 플랫폼 전체 약관 (조직별이 없으면 플랫폼 전체로 대체)
            return dslContext.selectFrom(TERMS)
                    .where(TERMS.ORG_ID.isNull().or(TERMS.ORG_ID.eq(orgId)))
                    .and(TERMS.IS_CURRENT.eq(true))
                    .fetchInto(Terms.class);
        }
    }

    /**
     * 약관 ID로 조회
     * org_id가 NULL이면 플랫폼 전체 약관이므로 org_id 필터링 안함
     *
     * @param termId 약관 ID
     * @return 약관 Optional (없으면 빈 Optional)
     */
    public Optional<Terms> fetchById(Long termId) {
        Long orgId = TenantContext.getOrgId();  // nullable
        if (orgId == null) {
            // 플랫폼 전체 약관만 조회 가능
            return dslContext.selectFrom(TERMS)
                    .where(TERMS.ID.eq(termId))
                    .and(TERMS.ORG_ID.isNull())
                    .fetchOptionalInto(Terms.class);
        } else {
            // 조직별 약관 + 플랫폼 전체 약관 조회 가능
            return dslContext.selectFrom(TERMS)
                    .where(TERMS.ID.eq(termId))
                    .and(TERMS.ORG_ID.isNull().or(TERMS.ORG_ID.eq(orgId)))
                    .fetchOptionalInto(Terms.class);
        }
    }

    /**
     * 조직 ID, 약관 타입, 버전으로 약관 조회
     *
     * @param orgId 조직 ID (NULL이면 플랫폼 전체 약관)
     * @param termType 약관 타입 (TOS, PRIVACY, MARKETING)
     * @param version 약관 버전
     * @return 약관 Optional (없으면 빈 Optional)
     */
    public Optional<Terms> fetchByOrgTypeVersion(Long orgId, TermType termType, String version) {
        if (orgId == null) {
            return dslContext.selectFrom(TERMS)
                    .where(TERMS.ORG_ID.isNull())
                    .and(TERMS.TERM_TYPE.eq(termType))
                    .and(TERMS.VERSION.eq(version))
                    .fetchOptionalInto(Terms.class);
        } else {
            return dslContext.selectFrom(TERMS)
                    .where(TERMS.ORG_ID.eq(orgId))
                    .and(TERMS.TERM_TYPE.eq(termType))
                    .and(TERMS.VERSION.eq(version))
                    .fetchOptionalInto(Terms.class);
        }
    }

    /**
     * 조직의 약관 버전 이력 조회
     *
     * @param orgId 조직 ID (NULL이면 플랫폼 전체 약관)
     * @param termType 약관 타입
     * @return 약관 버전 이력 리스트 (최신순)
     */
    public List<Terms> fetchVersionHistory(Long orgId, TermType termType) {
        if (orgId == null) {
            return dslContext.selectFrom(TERMS)
                    .where(TERMS.ORG_ID.isNull())
                    .and(TERMS.TERM_TYPE.eq(termType))
                    .orderBy(TERMS.EFFECTIVE_DATE.desc())
                    .fetchInto(Terms.class);
        } else {
            return dslContext.selectFrom(TERMS)
                    .where(TERMS.ORG_ID.eq(orgId))
                    .and(TERMS.TERM_TYPE.eq(termType))
                    .orderBy(TERMS.EFFECTIVE_DATE.desc())
                    .fetchInto(Terms.class);
        }
    }

    /**
     * 약관을 삽입합니다.
     * org_id는 명시적으로 설정 (NULL 가능, 플랫폼 전체 약관)
     *
     * @param term 삽입할 약관 엔티티
     * @return 삽입된 약관 (모든 필드 포함)
     */
    public Terms insert(Terms term) {
        // 현재 시간 설정
        LocalDateTime now = LocalDateTime.now();
        term.setCreatedAt(now);
        term.setModifiedAt(now);

        dao.insert(term);  // DAO 패턴
        return term;
    }

    /**
     * 특정 약관을 현재(current) 약관으로 설정합니다.
     * 트랜잭션 필수: 기존 is_current를 false로 변경 후 새 약관을 true로 설정
     *
     * UNIQUE 제약: (org_id, term_type) WHERE is_current = TRUE
     * 한 타입당 하나의 약관만 current 가능
     *
     * @param orgId 조직 ID (NULL이면 플랫폼 전체 약관)
     * @param termType 약관 타입
     * @param newTermId 새로운 현재 약관 ID
     */
    @Transactional
    public void updateCurrentTerm(Long orgId, TermType termType, Long newTermId) {
        // 1. 기존 current 약관을 false로 변경
        if (orgId == null) {
            dslContext.update(TERMS)
                    .set(TERMS.IS_CURRENT, false)
                    .set(TERMS.MODIFIED_AT, LocalDateTime.now())
                    .where(TERMS.ORG_ID.isNull())
                    .and(TERMS.TERM_TYPE.eq(termType))
                    .and(TERMS.IS_CURRENT.eq(true))
                    .execute();
        } else {
            dslContext.update(TERMS)
                    .set(TERMS.IS_CURRENT, false)
                    .set(TERMS.MODIFIED_AT, LocalDateTime.now())
                    .where(TERMS.ORG_ID.eq(orgId))
                    .and(TERMS.TERM_TYPE.eq(termType))
                    .and(TERMS.IS_CURRENT.eq(true))
                    .execute();
        }

        // 2. 새 약관을 current로 설정
        if (orgId == null) {
            dslContext.update(TERMS)
                    .set(TERMS.IS_CURRENT, true)
                    .set(TERMS.MODIFIED_AT, LocalDateTime.now())
                    .where(TERMS.ID.eq(newTermId))
                    .and(TERMS.ORG_ID.isNull())
                    .execute();
        } else {
            dslContext.update(TERMS)
                    .set(TERMS.IS_CURRENT, true)
                    .set(TERMS.MODIFIED_AT, LocalDateTime.now())
                    .where(TERMS.ID.eq(newTermId))
                    .and(TERMS.ORG_ID.eq(orgId))
                    .execute();
        }
    }

    /**
     * 약관을 삭제합니다.
     * 주의: 현재(current) 약관은 삭제할 수 없음 (비즈니스 로직에서 검증)
     *
     * @param termId 약관 ID
     * @return 영향받은 행 수
     */
    public int delete(Long termId) {
        Long orgId = TenantContext.getOrgId();  // nullable
        if (orgId == null) {
            return dslContext.deleteFrom(TERMS)
                    .where(TERMS.ID.eq(termId))
                    .and(TERMS.ORG_ID.isNull())
                    .and(TERMS.IS_CURRENT.eq(false))  // current 약관은 삭제 불가
                    .execute();
        } else {
            return dslContext.deleteFrom(TERMS)
                    .where(TERMS.ID.eq(termId))
                    .and(TERMS.ORG_ID.eq(orgId))
                    .and(TERMS.IS_CURRENT.eq(false))  // current 약관은 삭제 불가
                    .execute();
        }
    }
}
