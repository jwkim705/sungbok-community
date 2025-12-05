package com.sungbok.community.controller;

import com.sungbok.community.common.constant.UriConstant;
import com.sungbok.community.common.dto.OkResponseDTO;
import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.dto.auth.RefreshTokenRequest;
import com.sungbok.community.dto.auth.TokenResponse;
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
 * 토큰 갱신, 로그아웃, 사용자 정보 조회
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

    /**
     * POST /auth/refresh
     * Refresh Token으로 새로운 Access Token 발급
     *
     * @param request Refresh Token 요청
     * @return 새로운 Access Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<OkResponseDTO> refreshToken(@RequestBody @Valid RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // 1. Refresh Token 검증 (서명 및 만료 시간)
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(401).body(
                    OkResponseDTO.of(401, "Invalid or expired refresh token", null)
            );
        }

        // 2. Refresh Token에서 이메일 추출
        String email = jwtTokenProvider.getEmailFromToken(refreshToken);

        // 3. Redis에서 Refresh Token 존재 여부 확인
        if (!refreshTokenService.validateRefreshToken(email, refreshToken)) {
            return ResponseEntity.status(401).body(
                    OkResponseDTO.of(401, "Refresh token not found or revoked", null)
            );
        }

        // 4. 사용자 정보 조회
        UserMemberDTO user = userRepository.fetchUserWithDetailsByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

        // 5. 새로운 Access Token 생성
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)  // 기존 Refresh Token 유지
                .tokenType("Bearer")
                .expiresIn(900)  // 15분 (초 단위)
                .build();

        log.info("Access Token 갱신 완료: {}", email);
        return ResponseEntity.ok(OkResponseDTO.of(200, "Token refreshed successfully", tokenResponse));
    }

    /**
     * POST /auth/logout
     * 로그아웃 (Refresh Token 삭제)
     *
     * @param authentication 현재 인증된 사용자
     * @return 로그아웃 성공 메시지
     */
    @PostMapping("/logout")
    public ResponseEntity<OkResponseDTO> logout(Authentication authentication) {
        UserMemberDTO user = SecurityUtils.getUserFromAuthentication(authentication);
        refreshTokenService.deleteRefreshToken(user.getEmail());

        log.info("로그아웃 완료: {}", user.getEmail());
        return ResponseEntity.ok(OkResponseDTO.of(200, "Logged out successfully", null));
    }

    /**
     * GET /auth/me
     * 현재 인증된 사용자 정보 조회 (테스트용)
     *
     * @param authentication 현재 인증된 사용자
     * @return 사용자 정보
     */
    @GetMapping("/me")
    public ResponseEntity<OkResponseDTO> getCurrentUser(Authentication authentication) {
        UserMemberDTO user = SecurityUtils.getUserFromAuthentication(authentication);
        return ResponseEntity.ok(OkResponseDTO.of(200, "User info retrieved", user));
    }
}
