package com.sungbok.community.security.jwt;

import com.sungbok.community.common.exception.ResourceNotFoundException;
import com.sungbok.community.common.exception.ValidationException;
import com.sungbok.community.common.exception.code.TenantErrorCode;
import com.sungbok.community.common.exception.code.ValidationErrorCode;
import com.sungbok.community.repository.OrganizationsRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.jooq.generated.tables.pojos.Organizations;
import org.springframework.stereotype.Component;

/**
 * 테넌트(조직) 식별 해결 컴포넌트
 * X-Org-Id 헤더에서 org_id 추출 및 검증
 *
 * @since 0.0.1
 */
@Component
public class TenantResolver {

    private static final String ORG_ID_HEADER = "X-Org-Id";

    private final OrganizationsRepository organizationsRepository;

    public TenantResolver(OrganizationsRepository organizationsRepository) {
        this.organizationsRepository = organizationsRepository;
    }

    /**
     * HTTP 요청에서 org_id 추출 및 검증
     * 1. X-Org-Id 헤더 필수 확인
     * 2. 숫자 형식 검증
     * 3. 조직 존재 여부 및 is_public 확인
     *
     * @param request HTTP 요청
     * @return 검증된 org_id
     * @throws ValidationException X-Org-Id 헤더가 없거나 형식이 잘못된 경우
     * @throws ResourceNotFoundException 조직이 존재하지 않거나 비공개인 경우
     */
    public Long resolveOrgId(HttpServletRequest request) {
        String headerOrgId = request.getHeader(ORG_ID_HEADER);

        // 1. X-Org-Id 헤더 필수 검증
        if (headerOrgId == null || headerOrgId.isBlank()) {
            throw new ValidationException(
                ValidationErrorCode.FAILED,
                Map.of("X-Org-Id", "Guest 접근을 위해 X-Org-Id 헤더가 필요합니다")
            );
        }

        // 2. 숫자 형식 검증
        Long orgId;
        try {
            orgId = Long.parseLong(headerOrgId);
        } catch (NumberFormatException e) {
            throw new ValidationException(
                ValidationErrorCode.INVALID_FORMAT,
                Map.of("X-Org-Id", "유효한 숫자여야 합니다")
            );
        }

        // 3. 조직 존재 및 공개 여부 검증
        return organizationsRepository.fetchById(orgId)
                .filter(Organizations::getIsPublic)
                .map(Organizations::getOrgId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    TenantErrorCode.NOT_FOUND,
                    Map.of("orgId", orgId)
                ));
    }
}
