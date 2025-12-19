package com.sungbok.community.service.get;

import com.sungbok.community.dto.TermDTO;
import com.sungbok.community.dto.UserTermAgreementDTO;
import org.jooq.generated.enums.TermType;

import java.util.List;

/**
 * 약관 조회 서비스 (CQRS - Query)
 * 현재 유효한 약관 조회, 사용자 동의 이력 조회
 */
public interface GetTermsService {

    /**
     * 현재 유효한 약관 조회 (is_current = true)
     * 플랫폼 전체 약관 + 조직별 약관 포함
     *
     * @param orgId 조직 ID (NULL이면 플랫폼 전체 약관만 조회)
     * @return 현재 유효한 약관 리스트
     */
    List<TermDTO> getCurrentTerms(Long orgId);

    /**
     * 사용자의 약관 동의 이력 조회
     *
     * @param userId 사용자 ID
     * @return 약관 동의 이력 리스트 (JOIN하여 약관 정보 포함)
     */
    List<UserTermAgreementDTO> getUserAgreements(Long userId);

    /**
     * 사용자가 모든 필수 약관에 동의했는지 확인
     * 회원가입 시 검증용
     *
     * @param userId 사용자 ID
     * @param orgId 조직 ID
     * @return 모든 필수 약관 동의 여부
     */
    boolean hasAgreedToAllRequiredTerms(Long userId, Long orgId);

    /**
     * 특정 타입의 약관 버전 이력 조회
     * 관리자 전용
     *
     * @param orgId 조직 ID
     * @param termType 약관 타입
     * @return 약관 버전 이력 리스트 (최신순)
     */
    List<TermDTO> getTermVersionHistory(Long orgId, TermType termType);
}
