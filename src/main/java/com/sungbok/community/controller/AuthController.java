package com.sungbok.community.controller;

import com.sungbok.community.common.constant.UriConstant;
import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.dto.auth.*;
import com.sungbok.community.repository.UserRepository;
import com.sungbok.community.security.jwt.JwtTokenProvider;
import com.sungbok.community.security.jwt.RefreshTokenService;
import com.sungbok.community.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

/**
 * JWT 인증 컨트롤러
 * OAuth2 소셜 로그인, 토큰 갱신, 로그아웃, 사용자 정보 조회
 *
 * @since 0.0.1
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(UriConstant.AUTH)
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final com.sungbok.community.service.oauth.OAuthLoginService oauthLoginService;  // OAuth 2.1 Strategy Pattern

    /**
     * POST /auth/refresh
     * Refresh Token으로 새로운 Access Token 발급
     *
     * @param request Refresh Token 요청
     * @return 새로운 Access Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@RequestBody @Valid RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // 1. Refresh Token 검증 (서명 및 만료 시간)
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new com.sungbok.community.common.exception.AuthenticationException(
                    com.sungbok.community.common.exception.code.AuthErrorCode.INVALID_TOKEN
            );
        }

        // 2. Refresh Token에서 이메일 추출 (Refresh Token에는 email만 포함)
        String email = jwtTokenProvider.getEmailFromToken(refreshToken);

        // 3. Redis에서 Refresh Token 존재 여부 확인
        if (!refreshTokenService.validateRefreshToken(email, refreshToken)) {
            throw new com.sungbok.community.common.exception.AuthenticationException(
                    com.sungbok.community.common.exception.code.AuthErrorCode.TOKEN_NOT_FOUND
            );
        }

        // 4. 이메일로 사용자 정보 조회 (Guest JWT 발급: orgId=null)
        org.jooq.generated.tables.pojos.Users userPojo = userRepository.fetchByEmail(email)
                .orElseThrow(() -> new com.sungbok.community.common.exception.ResourceNotFoundException(
                        com.sungbok.community.common.exception.code.ResourceErrorCode.NOT_FOUND,
                        java.util.Map.of("email", email)
                ));

        // Guest JWT 생성 (orgId=null, roleIds=empty) - 필수 필드만 설정
        UserMemberDTO user = new UserMemberDTO(
                null,  // orgId = null (Guest)
                userPojo.getId(),  // userId
                userPojo.getEmail(),
                userPojo.getEmail(),  // name (users 테이블에 없음, email 사용)
                null,  // password (JWT에 불필요)
                null,  // birthday (Guest는 불필요)
                null,  // gender (Guest는 불필요)
                null,  // address (Guest는 불필요)
                null,  // phoneNumber (Guest는 불필요)
                null,  // picture (Guest는 불필요)
                null,  // registeredByUserId (Guest는 불필요)
                null,  // appTypeId = null (Guest)
                java.util.List.of()  // roleIds = empty (Guest)
        );

        // 5. 새로운 Access Token 생성
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)  // 기존 Refresh Token 유지
                .tokenType("Bearer")
                .expiresIn(900)  // 15분 (초 단위)
                .build();

        log.info("Access Token 갱신 완료: {}", email);
        return ResponseEntity.ok(tokenResponse);
    }

    /**
     * POST /auth/logout
     * 로그아웃 (Refresh Token 삭제)
     * Valkey 9 DELIFEQ로 Race Condition 방지
     *
     * @param authentication 현재 인증된 사용자
     * @param request        Refresh Token 포함
     * @return 로그아웃 성공 메시지
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        Authentication authentication,
        @RequestBody @Valid LogoutRequest request
    ) {
        UserMemberDTO user = SecurityUtils.getUserFromAuthentication(authentication);

        // DELIFEQ로 안전하게 삭제 (토큰 일치 시에만 삭제)
        boolean deleted = refreshTokenService.deleteRefreshTokenIfMatches(
            user.getEmail(),
            request.refreshToken()
        );

        if (!deleted) {
            throw new com.sungbok.community.common.exception.AuthenticationException(
                    com.sungbok.community.common.exception.code.AuthErrorCode.TOKEN_MISMATCH
            );
        }

        log.info("로그아웃 완료: {}", user.getEmail());
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /auth/me
     * 현재 인증된 사용자 정보 조회 (테스트용)
     *
     * @param authentication 현재 인증된 사용자
     * @return 사용자 정보
     */
    @GetMapping("/me")
    public ResponseEntity<UserMemberDTO> getCurrentUser(Authentication authentication) {
        UserMemberDTO user = SecurityUtils.getUserFromAuthentication(authentication);
        return ResponseEntity.ok(user);
    }

    /**
     * POST /auth/login/{provider}
     * OAuth2 소셜 로그인 (통합 엔드포인트, OAuth 2.1 표준)
     * Strategy Pattern 적용: 단일 엔드포인트로 모든 소셜 로그인 처리
     *
     * @param provider      소셜 공급자 (google, kakao, naver)
     * @param request       OAuth2 Authorization Code
     * @param httpRequest   HTTP 요청 (세션 메타데이터 추출용)
     * @return JWT 토큰 (Access + Refresh)
     */
    @PostMapping("/login/{provider}")
    public ResponseEntity<TokenResponse> loginWithOAuth(
        @PathVariable String provider,
        @RequestBody @Valid OAuth2CodeRequest request,
        jakarta.servlet.http.HttpServletRequest httpRequest) {

        // OAuthLoginService가 provider에 맞는 구현체를 찾아서 실행
        TokenResponse tokenResponse = oauthLoginService.login(
            provider,
            request.getCode(),
            request.getCodeVerifier(),
            httpRequest
        );

        return ResponseEntity.ok(tokenResponse);
    }
}
