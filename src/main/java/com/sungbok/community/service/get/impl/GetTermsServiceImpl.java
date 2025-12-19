package com.sungbok.community.service.get.impl;

import com.sungbok.community.dto.TermDTO;
import com.sungbok.community.dto.UserTermAgreementDTO;
import com.sungbok.community.repository.TermsRepository;
import com.sungbok.community.repository.UserTermAgreementsRepository;
import com.sungbok.community.service.get.GetTermsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.generated.enums.TermType;
import org.jooq.generated.tables.pojos.Terms;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 약관 조회 서비스 구현체
 * 현재 유효한 약관 조회, 사용자 동의 이력 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetTermsServiceImpl implements GetTermsService {

    private final TermsRepository termsRepository;
    private final UserTermAgreementsRepository userTermAgreementsRepository;

    @Override
    public List<TermDTO> getCurrentTerms(Long orgId) {
        List<Terms> terms = termsRepository.fetchCurrentTerms(orgId);
        return terms.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserTermAgreementDTO> getUserAgreements(Long userId) {
        // Repository의 JOIN 메서드 사용 (termTitle, termVersion 포함)
        return userTermAgreementsRepository.fetchByUserIdWithTermInfo(userId);
    }

    @Override
    public boolean hasAgreedToAllRequiredTerms(Long userId, Long orgId) {
        // 현재 유효한 필수 약관 ID 조회
        List<Long> requiredTermIds = termsRepository.fetchCurrentTerms(orgId)
                .stream()
                .filter(Terms::getIsRequired)
                .map(Terms::getId)
                .collect(Collectors.toList());

        if (requiredTermIds.isEmpty()) {
            return true;
        }

        return userTermAgreementsRepository.hasAgreedToAllRequiredTerms(userId, requiredTermIds);
    }

    @Override
    public List<TermDTO> getTermVersionHistory(Long orgId, TermType termType) {
        List<Terms> terms = termsRepository.fetchVersionHistory(orgId, termType);
        return terms.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
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
