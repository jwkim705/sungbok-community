package com.sungbok.community.security.handler;

import com.sungbok.community.common.dto.OkResponseDTO;
import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.dto.auth.LoginResponse;
import com.sungbok.community.dto.auth.TokenResponse;
import com.sungbok.community.security.jwt.JwtTokenProvider;
import com.sungbok.community.security.jwt.RefreshTokenService;
import com.sungbok.community.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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

  @Override
  public void onAuthenticationSuccess(@NonNull HttpServletRequest request,
                                      @NonNull HttpServletResponse response,
                                      @NonNull Authentication authentication) throws IOException {

    UserMemberDTO userMember = SecurityUtils.getUserFromAuthentication(authentication);

    log.info("Form 로그인 성공: {}", userMember.getEmail());

    // Form 로그인: JWT 토큰 생성 후 JSON 응답
    String accessToken = jwtTokenProvider.generateAccessToken(userMember);
    String refreshToken = jwtTokenProvider.generateRefreshToken(userMember.getEmail());
    refreshTokenService.saveRefreshToken(userMember.getEmail(), refreshToken);

    handleFormLoginSuccess(response, userMember, accessToken, refreshToken);
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

    // OkResponseDTO로 래핑
    OkResponseDTO responseDTO = OkResponseDTO.of(
        HttpStatus.OK.value(),
        "Login successful",
        loginResponse
    );

    // JSON 응답 반환
    response.setStatus(HttpStatus.OK.value());
    response.setCharacterEncoding("UTF-8");
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(response.getWriter(), responseDTO);

    log.info("Form 로그인 성공, JSON 응답 반환 (사용자: {})", userMember.getEmail());
  }
}
