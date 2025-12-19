package com.sungbok.community.service.change;

import com.sungbok.community.dto.AgreeToTermsRequest;
import com.sungbok.community.dto.CreateTermRequest;
import com.sungbok.community.dto.TermDTO;
import org.jooq.generated.enums.TermType;

/**
 * 약관 변경 서비스 (CQRS - Command)
 * 약관 동의 처리, 약관 생성/관리 (관리자)
 */
public interface ChangeTermsService {

    /**
     * 사용자 약관 동의 처리
     * 회원가입 시 여러 약관 동의를 Batch INSERT
     *
     * @param userId 사용자 ID
     * @param request 동의할 약관 ID 리스트
     * @param ipAddress 동의 시 IP 주소 (법적 증거)
     */
    void agreeToTerms(Long userId, AgreeToTermsRequest request, String ipAddress);

    /**
     * 새로운 약관 버전 생성
     * 관리자 전용 - 기존 약관이 있어도 새 버전으로 추가
     *
     * @param orgId 조직 ID (NULL이면 플랫폼 전체 약관)
     * @param request 약관 정보 (termType, version, title, content 등)
     * @return 생성된 약관 정보
     */
    TermDTO createTerm(Long orgId, CreateTermRequest request);

    /**
     * 특정 약관을 현재 버전으로 설정
     * 관리자 전용 - 트랜잭션 내에서 기존 current를 false로 변경 후 새 약관을 true로 설정
     *
     * @param orgId 조직 ID
     * @param termType 약관 타입
     * @param termId 새로운 현재 약관 ID
     */
    void setCurrentTerm(Long orgId, TermType termType, Long termId);

    /**
     * 약관 삭제
     * 관리자 전용 - 현재(current) 약관은 삭제 불가
     *
     * @param orgId 조직 ID
     * @param termId 약관 ID
     */
    void deleteTerm(Long orgId, Long termId);
}
