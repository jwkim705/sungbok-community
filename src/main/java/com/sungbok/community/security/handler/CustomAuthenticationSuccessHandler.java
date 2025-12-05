package com.sungbok.community.security.handler;

import com.sungbok.community.common.dto.OkResponseDTO;
import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.dto.auth.LoginResponse;
import com.sungbok.community.dto.auth.TokenResponse;
import com.sungbok.community.security.jwt.JwtTokenProvider;
import com.sungbok.community.security.jwt.RefreshTokenService;
import com.sungbok.community.config.CorsConfig;
import com.sungbok.community.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
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
  private final CorsConfig corsConfig;

  @Value("${app.frontend.default-url}")
  private String defaultFrontendUrl;

  @Value("${app.frontend.oauth-callback-path}")
  private String oauthCallbackPath;

  @Override
  public void onAuthenticationSuccess(@NonNull HttpServletRequest request,
                                      @NonNull HttpServletResponse response,
                                      @NonNull Authentication authentication) throws IOException {

    UserMemberDTO userMember = SecurityUtils.getUserFromAuthentication(authentication);

    log.info("SuccessHandler loginUser: {}", objectMapper.writeValueAsString(userMember));

    // JWT 토큰 생성
    String accessToken = jwtTokenProvider.generateAccessToken(userMember);
    String refreshToken = jwtTokenProvider.generateRefreshToken(userMember.getEmail());

    // Refresh Token을 Redis에 저장
    refreshTokenService.saveRefreshToken(userMember.getEmail(), refreshToken);

    // Authentication 타입에 따라 응답 분기
    if (authentication instanceof OAuth2AuthenticationToken) {
      // OAuth2 로그인: 프론트엔드로 리다이렉트 (기존 로직)
      handleOAuth2Success(request, response, userMember, accessToken, refreshToken);
    } else {
      // Form 로그인: JSON 응답 반환
      handleFormLoginSuccess(response, userMember, accessToken, refreshToken);
    }
  }

  /**
   * OAuth2 로그인 성공 처리: 프론트엔드로 리다이렉트 (토큰 쿼리 파라미터)
   */
  private void handleOAuth2Success(HttpServletRequest request,
                                   HttpServletResponse response,
                                   UserMemberDTO userMember,
                                   String accessToken,
                                   String refreshToken) throws IOException {
    // 프론트엔드로 리다이렉트 (동적 URL 감지)
    String referer = request.getHeader("Referer");
    String origin = request.getHeader("Origin");
    String targetFrontendUrl = determineFrontendUrl(referer, origin);

    // 리다이렉트 URL 구성
    String redirectUrl = String.format(
        "%s%s?access_token=%s&refresh_token=%s&token_type=Bearer&expires_in=%d",
        targetFrontendUrl,
        oauthCallbackPath,
        accessToken,
        refreshToken,
        900
    );

    log.info("OAuth2 로그인 성공, 프론트엔드로 리다이렉트: {} (사용자: {})",
        redirectUrl, userMember.getEmail());

    response.sendRedirect(redirectUrl);
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

  /**
   * 요청 헤더에서 프론트엔드 URL을 판단
   * Origin 헤더를 우선 사용하고, 없으면 Referer 헤더에서 추출
   * 둘 다 없거나 허용되지 않은 origin이면 기본 URL 반환
   */
  private String determineFrontendUrl(String referer, String origin) {
    // 1. Origin 헤더 우선 사용
    if (origin != null && corsConfig.getAllowedOrigins().contains(origin)) {
      log.debug("Origin 헤더에서 프론트엔드 URL 감지: {}", origin);
      return origin;
    }

    // 2. Referer 헤더에서 origin 추출
    if (referer != null) {
      try {
        URL url = (new URI(referer)).toURL();
        String extractedOrigin = url.getProtocol() + "://" + url.getAuthority();
        if (corsConfig.getAllowedOrigins().contains(extractedOrigin)) {
          log.debug("Referer 헤더에서 프론트엔드 URL 감지: {}", extractedOrigin);
          return extractedOrigin;
        }
      } catch (MalformedURLException e) {
        log.warn("Invalid referer URL: {}", referer);
      } catch (URISyntaxException e) {
          throw new RuntimeException(e);
      }
    }

    // 3. 기본 URL 사용 (fallback)
    log.info("프론트엔드 origin을 찾을 수 없어 기본 URL 사용: {}", defaultFrontendUrl);
    return defaultFrontendUrl;
  }
}
