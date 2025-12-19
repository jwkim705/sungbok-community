package com.sungbok.community.security.handler;

import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.dto.auth.LoginResponse;
import com.sungbok.community.dto.auth.TokenResponse;
import com.sungbok.community.repository.UserRepository;
import com.sungbok.community.security.TenantContext;
import com.sungbok.community.security.jwt.JwtTokenProvider;
import com.sungbok.community.security.jwt.RefreshTokenService;
import com.sungbok.community.security.jwt.TenantResolver;
import com.sungbok.community.util.SecurityUtils;
import com.sungbok.community.common.exception.ResourceNotFoundException;
import com.sungbok.community.common.exception.code.TenantErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  private final ObjectMapper objectMapper;
  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenService refreshTokenService;
  private final UserRepository userRepository;
  private final TenantResolver tenantResolver;

  @Override
  public void onAuthenticationSuccess(@NonNull HttpServletRequest request,
                                      @NonNull HttpServletResponse response,
                                      @NonNull Authentication authentication) throws IOException {

    UserMemberDTO tempUser = SecurityUtils.getUserFromAuthentication(authentication);

    log.info("Form 로그인 성공: {}", tempUser.getEmail());

    // 1. TenantResolver로 org_id 추출 및 검증 (존재 여부, 공개 여부 확인)
    Long orgId = tenantResolver.resolveOrgId(request);

    // 2. TenantContext 설정 (일관성: JWT 필터와 동일)
    TenantContext.setOrgId(orgId);
    try {
      // 3. fetchUserForLogin으로 사용자 정보 조회 (멤버십 여부 관계없이)
      UserMemberDTO userMember = userRepository.fetchUserForLogin(tempUser.getUserId(), orgId);
      if (userMember == null) {
        throw new ResourceNotFoundException(
            TenantErrorCode.NOT_FOUND,
            Map.of("orgId", orgId)
        );
      }

      // 4. JWT 생성
      String accessToken = jwtTokenProvider.generateAccessToken(userMember);
      String refreshToken = jwtTokenProvider.generateRefreshToken(userMember.getEmail());
      refreshTokenService.saveRefreshToken(userMember.getEmail(), refreshToken);

      log.info("Form 로그인 JWT 발급: userId={}, orgId={}, appTypeId={}, isMember={}",
          userMember.getUserId(), userMember.getOrgId(), userMember.getAppTypeId(),
          !userMember.getRoleIds().isEmpty());

      handleFormLoginSuccess(response, userMember, accessToken, refreshToken);
    } finally {
      TenantContext.clear();
    }
  }

  /**
   * Form 로그인 성공 처리: JSON 응답 반환
   */
  private void handleFormLoginSuccess(HttpServletResponse response,
                                      UserMemberDTO userMember,
                                      String accessToken,
                                      String refreshToken) throws IOException {
    // LoginResponse DTO 생성
    LoginResponse loginResponse = LoginResponse.builder()
        .user(userMember)
        .tokens(TokenResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(900)  // 15분 (초 단위)
            .build())
        .build();

    // JSON 응답 반환 (래퍼 없이 직접 반환)
    response.setStatus(HttpStatus.OK.value());
    response.setCharacterEncoding("UTF-8");
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(response.getWriter(), loginResponse);

    log.info("Form 로그인 성공, JSON 응답 반환 (사용자: {})", userMember.getEmail());
  }
}
