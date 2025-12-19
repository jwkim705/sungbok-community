package com.sungbok.community.support;

import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.security.jwt.JwtTokenProvider;
import com.sungbok.community.security.jwt.RefreshTokenService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT 토큰 추출 및 검증 헬퍼 클래스
 * 3개 테스트 파일의 중복 코드 제거
 */
@Component
public class TokenTestHelper {

    private final ObjectMapper objectMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final StringRedisTemplate redisTemplate;

    public TokenTestHelper(
            ObjectMapper objectMapper,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenService refreshTokenService,
            StringRedisTemplate redisTemplate
    ) {
        this.objectMapper = objectMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * MockMvc 응답에서 Access Token 추출
     *
     * @param result MockMvc 실행 결과
     * @return Access Token
     * @throws Exception JSON 파싱 실패 시
     */
    public String extractAccessToken(MvcResult result) throws Exception {
        String responseBody = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(responseBody);
        return root.get("accessToken").asString();
    }

    /**
     * MockMvc 응답에서 Refresh Token 추출
     *
     * @param result MockMvc 실행 결과
     * @return Refresh Token
     * @throws Exception JSON 파싱 실패 시
     */
    public String extractRefreshToken(MvcResult result) throws Exception {
        String responseBody = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(responseBody);
        return root.get("refreshToken").asString();
    }

    /**
     * Form Login 응답에서 토큰 추출
     *
     * @param result MockMvc 실행 결과
     * @return TokenPair (Access Token, Refresh Token)
     * @throws Exception JSON 파싱 실패 시
     */
    public TokenPair extractTokensFromFormLogin(MvcResult result) throws Exception {
        String responseBody = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(responseBody);
        String accessToken = root.get("tokens").get("accessToken").asString();
        String refreshToken = root.get("tokens").get("refreshToken").asString();
        return new TokenPair(accessToken, refreshToken);
    }

    /**
     * Access Token 전체 검증
     *
     * @param token        검증할 Access Token
     * @param expectedUser 예상되는 사용자 정보
     */
    public void assertValidAccessToken(String token, UserMemberDTO expectedUser) {
        assertTrue(jwtTokenProvider.validateToken(token), "Access token이 유효해야 함");

        assertEquals(expectedUser.getEmail(), jwtTokenProvider.getEmailFromToken(token),
                "Email이 일치해야 함");
        assertEquals(expectedUser.getUserId(), jwtTokenProvider.getUserIdFromToken(token),
                "User ID가 일치해야 함");
        assertThat(jwtTokenProvider.getRoleIdsFromToken(token))
                .containsExactlyInAnyOrderElementsOf(expectedUser.getRoleIds());
        assertEquals(expectedUser.getOrgId(), jwtTokenProvider.getOrgIdFromToken(token),
                "Org ID가 일치해야 함");
        assertEquals(expectedUser.getAppTypeId(), jwtTokenProvider.getAppTypeIdFromToken(token),
                "AppType ID가 일치해야 함");

        // 만료 시간 검증 (15분 = 900초)
        Date expiration = jwtTokenProvider.getExpirationFromToken(token);
        long expiresInSeconds = (expiration.getTime() - System.currentTimeMillis()) / 1000;
        assertTrue(expiresInSeconds > 850 && expiresInSeconds <= 900,
                "Access token은 약 15분 후 만료되어야 함 (실제: " + expiresInSeconds + "초)");
    }

    /**
     * Refresh Token 검증
     *
     * @param token         검증할 Refresh Token
     * @param expectedEmail 예상되는 이메일
     */
    public void assertValidRefreshToken(String token, String expectedEmail) {
        assertTrue(jwtTokenProvider.validateToken(token), "Refresh token이 유효해야 함");
        assertEquals(expectedEmail, jwtTokenProvider.getEmailFromToken(token),
                "Email이 일치해야 함");

        // Refresh Token은 더 긴 만료 시간을 가짐 (7일)
        Date expiration = jwtTokenProvider.getExpirationFromToken(token);
        long expiresInDays = (expiration.getTime() - System.currentTimeMillis()) / (1000 * 60 * 60 * 24);
        assertTrue(expiresInDays >= 6 && expiresInDays <= 7,
                "Refresh token은 약 7일 후 만료되어야 함 (실제: " + expiresInDays + "일)");
    }

    /**
     * Redis에 Refresh Token 저장 확인
     *
     * @param email         사용자 이메일
     * @param expectedToken 예상되는 토큰 값
     */
    public void assertRefreshTokenStoredInRedis(String email, String expectedToken) {
        String storedToken = refreshTokenService.getRefreshToken(email);
        assertNotNull(storedToken, "Refresh token이 Redis에 저장되어 있어야 함");
        assertEquals(expectedToken, storedToken, "저장된 토큰이 예상 토큰과 일치해야 함");

        // TTL 검증
        String key = "refresh_token:" + email;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        assertNotNull(ttl, "TTL이 설정되어 있어야 함");
        assertTrue(ttl > 0, "TTL이 양수여야 함");
        assertTrue(ttl <= 7 * 24 * 60 * 60, "TTL이 7일 이하여야 함");
    }

    /**
     * Redis에서 Refresh Token 삭제 확인
     *
     * @param email 사용자 이메일
     */
    public void assertRefreshTokenNotInRedis(String email) {
        String storedToken = refreshTokenService.getRefreshToken(email);
        assertNull(storedToken, "Refresh token이 Redis에 없어야 함");
    }

    /**
     * 토큰 쌍 DTO
     *
     * @param accessToken  Access Token
     * @param refreshToken Refresh Token
     */
    public record TokenPair(String accessToken, String refreshToken) {
    }
}
