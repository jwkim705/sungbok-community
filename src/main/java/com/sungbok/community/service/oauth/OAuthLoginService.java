package com.sungbok.community.service.oauth;

import com.sungbok.community.common.exception.ResourceNotFoundException;
import com.sungbok.community.common.exception.ValidationException;
import com.sungbok.community.common.exception.code.TenantErrorCode;
import com.sungbok.community.common.exception.code.ValidationErrorCode;
import com.sungbok.community.dto.SocialUserInfo;
import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.dto.auth.TokenResponse;
import com.sungbok.community.enums.SocialType;
import com.sungbok.community.repository.UserRepository;
import com.sungbok.community.security.jwt.JwtTokenProvider;
import com.sungbok.community.security.jwt.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.generated.tables.pojos.Users;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthLoginService {

    private final List<SocialLoginService> socialLoginServices;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    public TokenResponse login(String providerName, String code, String codeVerifier, HttpServletRequest request) {
        SocialType socialType = SocialType.fromString(providerName);

        SocialLoginService socialService = socialLoginServices.stream()
            .filter(service -> service.getServiceName() == socialType)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 소셜 로그인입니다: " + providerName));

        // 1. OAuth 인증
        String socialAccessToken = socialService.getSocialAccessToken(code, codeVerifier);
        SocialUserInfo socialUserInfo = socialService.getSocialUserInfo(socialAccessToken);

        // 1-1. Email null 검증 (소셜 제공자가 이메일을 제공하지 않을 수 있음)
        if (socialUserInfo.getEmail() == null || socialUserInfo.getEmail().isBlank()) {
            throw new IllegalArgumentException(
                String.format("%s에서 이메일을 제공하지 않았습니다. 계정 설정에서 이메일 동의를 확인하세요.", socialType)
            );
        }

        // 2. Users 테이블 upsert (플랫폼 레벨)
        Users user = userRepository.upsertOAuthUser(
            socialUserInfo.getEmail(),
            socialUserInfo.getName(),
            socialUserInfo.getPicture()
        );

        // 3. X-Org-Id 헤더에서 orgId 추출 및 검증
        String orgIdHeader = request.getHeader("X-Org-Id");
        if (orgIdHeader == null) {
            throw new IllegalArgumentException("X-Org-Id 헤더가 필요합니다");
        }

        Long orgId;
        try {
            orgId = Long.parseLong(orgIdHeader);
            if (orgId <= 0) {
                throw new IllegalArgumentException("조직 ID는 양수여야 합니다");
            }
        } catch (NumberFormatException e) {
            throw new ValidationException(
                ValidationErrorCode.INVALID_FORMAT,
                Map.of("X-Org-Id", orgIdHeader)
            );
        }

        // 4. fetchUserForLogin으로 사용자 정보 조회 (멤버십 여부 관계없이)
        UserMemberDTO userDto = userRepository.fetchUserForLogin(user.getId(), orgId);
        if (userDto == null) {
            throw new ResourceNotFoundException(
                TenantErrorCode.NOT_FOUND,
                Map.of("orgId", orgId)
            );
        }

        // 5. JWT 발급
        String accessToken = jwtTokenProvider.generateAccessToken(userDto);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDto.getEmail());

        // Refresh Token 저장
        refreshTokenService.saveRefreshToken(userDto.getEmail(), refreshToken);

        log.info("{} 로그인 성공: userId={}, orgId={}, email={}, isMember={}",
            socialType, userDto.getUserId(), userDto.getOrgId(),
            userDto.getEmail(), !userDto.getRoleIds().isEmpty());

        return TokenResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(900)
            .build();
    }
}
