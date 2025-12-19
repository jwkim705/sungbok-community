package com.sungbok.community.service.change.impl;

import com.sungbok.community.common.exception.ResourceNotFoundException;
import com.sungbok.community.common.exception.ValidationException;
import com.sungbok.community.common.exception.code.ResourceErrorCode;
import com.sungbok.community.common.exception.code.ValidationErrorCode;
import com.sungbok.community.dto.AgreeToTermsRequest;
import com.sungbok.community.dto.CreateTermRequest;
import com.sungbok.community.dto.TermDTO;
import com.sungbok.community.repository.TermsRepository;
import com.sungbok.community.repository.UserTermAgreementsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.generated.enums.TermType;
import org.jooq.generated.tables.pojos.Terms;
import org.jooq.generated.tables.pojos.UserTermAgreements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 약관 변경 서비스 구현체
 * 약관 동의 처리, 약관 생성/관리 (관리자)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChangeTermsServiceImpl implements com.sungbok.community.service.change.ChangeTermsService {

    private final TermsRepository termsRepository;
    private final UserTermAgreementsRepository userTermAgreementsRepository;

    @Override
    public void agreeToTerms(Long userId, AgreeToTermsRequest request, String ipAddress) {
        List<UserTermAgreements> agreements = request.getTermIds().stream()
                .map(termId -> {
                    UserTermAgreements agreement = new UserTermAgreements();
                    agreement.setUserId(userId);
                    agreement.setTermId(termId);
                    agreement.setAgreedAt(LocalDateTime.now());
                    if (ipAddress != null) {
                        // INET 타입 변환은 jOOQ가 자동 처리
                        agreement.setIpAddress(org.jooq.types.UByte.valueOf(ipAddress));
                    }
                    return agreement;
                })
                .collect(Collectors.toList());

        userTermAgreementsRepository.batchInsert(agreements);
        log.info("User {} agreed to {} terms", userId, agreements.size());
    }

    @Override
    public TermDTO createTerm(Long orgId, CreateTermRequest request) {
        Terms term = new Terms();
        term.setOrgId(orgId);
        term.setTermType(request.getTermType());
        term.setVersion(request.getVersion());
        term.setTitle(request.getTitle());
        term.setContent(request.getContent());
        term.setIsRequired(request.getIsRequired());
        term.setIsCurrent(false);  // 생성 시점에는 false, 명시적으로 설정해야 함
        term.setEffectiveDate(request.getEffectiveDate());

        Terms savedTerm = termsRepository.insert(term);
        log.info("Created new term: orgId={}, termType={}, version={}", orgId, request.getTermType(), request.getVersion());

        return convertToDTO(savedTerm);
    }

    @Override
    public void setCurrentTerm(Long orgId, TermType termType, Long termId) {
        // 약관 존재 여부 확인
        termsRepository.fetchById(termId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ResourceErrorCode.NOT_FOUND,
                        Map.of("termId", termId)
                ));

        // 트랜잭션 내에서 기존 current를 false로 변경 후 새 약관을 true로 설정
        termsRepository.updateCurrentTerm(orgId, termType, termId);
        log.info("Set current term: orgId={}, termType={}, termId={}", orgId, termType, termId);
    }

    @Override
    public void deleteTerm(Long orgId, Long termId) {
        int deletedRows = termsRepository.delete(termId);

        if (deletedRows == 0) {
            throw new ValidationException(
                    ValidationErrorCode.FAILED,
                    Map.of("error", "현재(current) 약관은 삭제할 수 없거나 약관이 존재하지 않습니다.")
            );
        }

        log.info("Deleted term: orgId={}, termId={}", orgId, termId);
    }

    /**
     * Terms 엔티티를 TermDTO로 변환
     */
    private TermDTO convertToDTO(Terms term) {
        return TermDTO.builder()
                .id(term.getId())
                .orgId(term.getOrgId())
                .termType(term.getTermType())
                .version(term.getVersion())
                .title(term.getTitle())
                .content(term.getContent())
                .isRequired(term.getIsRequired())
                .isCurrent(term.getIsCurrent())
                .effectiveDate(term.getEffectiveDate())
                .build();
    }
}
