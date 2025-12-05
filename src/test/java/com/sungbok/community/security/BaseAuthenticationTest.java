package com.sungbok.community.security;

import com.sungbok.community.dto.AddUserRequestDTO;
import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.enums.UserRole;
import com.sungbok.community.repository.UserRepository;
import com.sungbok.community.security.jwt.JwtTokenProvider;
import com.sungbok.community.security.jwt.RefreshTokenService;
import com.sungbok.community.service.change.ChangeUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Authentication 테스트를 위한 Base 클래스
 * 공통 헬퍼 메서드와 테스트 픽스처를 제공합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseAuthenticationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtTokenProvider jwtTokenProvider;

    @Autowired
    protected RefreshTokenService refreshTokenService;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected ChangeUserService changeUserService;

    @Autowired
    protected StringRedisTemplate redisTemplate;

    protected static final String TEST_PASSWORD = "Password123!";

    /**
     * 각 테스트 전에 TenantContext를 초기화합니다.
     * 테스트용 기본 테넌트(app_id = 1: Default Church)를 설정합니다.
     */
    @BeforeEach
    void setUpTenantContext() {
        TenantContext.setAppId(1L);
    }

    /**
     * 각 테스트 후 Redis에서 refresh token을 정리하고 TenantContext를 정리합니다.
     */
    @AfterEach
    void cleanupRedis() {
        // Redis refresh token 정리
        Set<String> keys = redisTemplate.keys("refresh_token:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        // TenantContext 정리 (ThreadLocal 메모리 누수 방지)
        TenantContext.clear();
    }

    /**
     * 테스트 사용자를 생성합니다.
     *
     * @param email    사용자 이메일
     * @param password 사용자 비밀번호 (null이면 OAuth2 사용자)
     * @param role     사용자 역할
     * @return 생성된 UserMemberDTO
     */
    protected UserMemberDTO createTestUser(String email, String password, UserRole role) {
        // 이미 존재하는 사용자인지 확인
        UserMemberDTO existing = userRepository.fetchUserWithDetailsByEmail(email).orElse(null);
        if (existing != null) {
            return existing;
        }

        // 새로운 사용자 생성
        AddUserRequestDTO requestDTO = AddUserRequestDTO.builder()
                .email(email)
                .password(password != null ? password : "DefaultPassword123!")
                .name(email.split("@")[0]) // 이메일의 첫 부분을 이름으로 사용
                .nickname(email.split("@")[0] + "_nick")
                .birthday(LocalDate.of(1990, 1, 1))
                .gender("M")
                .address("Test Address")
                .phoneNumber("010-1234-5678")
                .deptNm("테스트부")
                .role(role.getCode())
                .build();

        return changeUserService.signup(requestDTO);
    }

    /**
     * Access Token이 유효한지 검증합니다.
     *
     * @param token        검증할 Access Token
     * @param expectedUser 예상되는 사용자 정보
     */
    protected void assertValidAccessToken(String token, UserMemberDTO expectedUser) {
        assertTrue(jwtTokenProvider.validateToken(token), "Access token should be valid");

        assertEquals(expectedUser.getEmail(), jwtTokenProvider.getEmailFromToken(token),
                "Email should match");
        assertEquals(expectedUser.getUserId(), jwtTokenProvider.getUserIdFromToken(token),
                "User ID should match");
        assertEquals(expectedUser.getRole().getCode(), jwtTokenProvider.getRoleFromToken(token),
                "Role should match");

        // 만료 시간 검증 (15분 = 900초)
        Date expiration = jwtTokenProvider.getExpirationFromToken(token);
        long expiresInSeconds = (expiration.getTime() - System.currentTimeMillis()) / 1000;
        assertTrue(expiresInSeconds > 850 && expiresInSeconds <= 900,
                "Access token should expire in ~15 minutes (got " + expiresInSeconds + " seconds)");
    }

    /**
     * Refresh Token이 유효한지 검증합니다.
     *
     * @param token         검증할 Refresh Token
     * @param expectedEmail 예상되는 이메일
     */
    protected void assertValidRefreshToken(String token, String expectedEmail) {
        assertTrue(jwtTokenProvider.validateToken(token), "Refresh token should be valid");
        assertEquals(expectedEmail, jwtTokenProvider.getEmailFromToken(token),
                "Email should match");

        // Refresh Token은 더 긴 만료 시간을 가짐 (7일)
        Date expiration = jwtTokenProvider.getExpirationFromToken(token);
        long expiresInDays = (expiration.getTime() - System.currentTimeMillis()) / (1000 * 60 * 60 * 24);
        assertTrue(expiresInDays >= 6 && expiresInDays <= 7,
                "Refresh token should expire in ~7 days (got " + expiresInDays + " days)");
    }

    /**
     * Refresh Token이 Redis에 저장되어 있는지 확인합니다.
     *
     * @param email         사용자 이메일
     * @param expectedToken 예상되는 토큰 값
     */
    protected void assertRefreshTokenStoredInRedis(String email, String expectedToken) {
        String storedToken = refreshTokenService.getRefreshToken(email);
        assertNotNull(storedToken, "Refresh token should be stored in Redis");
        assertEquals(expectedToken, storedToken, "Stored token should match expected token");

        // TTL 검증
        String key = "refresh_token:" + email;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        assertNotNull(ttl, "TTL should be set");
        assertTrue(ttl > 0, "TTL should be positive");
        assertTrue(ttl <= 7 * 24 * 60 * 60, "TTL should be <= 7 days");
    }

    /**
     * Refresh Token이 Redis에서 삭제되었는지 확인합니다.
     *
     * @param email 사용자 이메일
     */
    protected void assertRefreshTokenNotInRedis(String email) {
        String storedToken = refreshTokenService.getRefreshToken(email);
        assertNull(storedToken, "Refresh token should not be in Redis");
    }
}
