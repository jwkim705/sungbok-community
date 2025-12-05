package com.sungbok.community.security.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Refresh Token 관리 서비스
 * Redis에 Refresh Token 저장/조회/삭제
 *
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtProperties jwtProperties;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    /**
     * Refresh Token을 Redis에 저장 (7일 TTL)
     *
     * @param email         사용자 이메일
     * @param refreshToken  Refresh Token
     */
    public void saveRefreshToken(String email, String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + email;
        stringRedisTemplate.opsForValue().set(
                key,
                refreshToken,
                jwtProperties.getRefreshTokenExpiration(),
                TimeUnit.MILLISECONDS
        );
        log.debug("Refresh Token 저장 완료: {}", email);
    }

    /**
     * Redis에서 Refresh Token 조회
     *
     * @param email 사용자 이메일
     * @return Refresh Token (없으면 null)
     */
    public String getRefreshToken(String email) {
        String key = REFRESH_TOKEN_PREFIX + email;
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * Refresh Token 유효성 검증 (Redis에 존재하고 값이 일치하는지)
     *
     * @param email         사용자 이메일
     * @param refreshToken  검증할 Refresh Token
     * @return 유효 여부
     */
    public boolean validateRefreshToken(String email, String refreshToken) {
        String storedToken = getRefreshToken(email);
        boolean isValid = storedToken != null && storedToken.equals(refreshToken);
        log.debug("Refresh Token 검증 결과 - {}: {}", email, isValid);
        return isValid;
    }

    /**
     * Refresh Token 삭제 (로그아웃)
     *
     * @param email 사용자 이메일
     */
    public void deleteRefreshToken(String email) {
        String key = REFRESH_TOKEN_PREFIX + email;
        stringRedisTemplate.delete(key);
        log.info("Refresh Token 삭제 완료: {}", email);
    }

    /**
     * 사용자의 모든 Refresh Token 삭제 (전체 기기 로그아웃)
     * 현재는 단일 토큰만 지원하므로 deleteRefreshToken과 동일
     * 향후 다중 디바이스 지원 시 확장 가능
     *
     * @param email 사용자 이메일
     */
    public void deleteAllRefreshTokens(String email) {
        deleteRefreshToken(email);
        // TODO: 향후 다중 디바이스 지원 시 확장
        // Set<String> keys = stringRedisTemplate.keys(REFRESH_TOKEN_PREFIX + email + ":*");
        // if (keys != null && !keys.isEmpty()) {
        //     stringRedisTemplate.delete(keys);
        // }
    }
}
