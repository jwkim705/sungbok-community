package com.sungbok.community.security.jwt;

import com.sungbok.community.dto.UserMemberDTO;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * JWT 토큰 생성 및 검증 Provider
 * ES256 (ECDSA P-256) 알고리즘 사용
 *
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final ResourceLoader resourceLoader;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    /**
     * 애플리케이션 시작 시 EC 키 페어 로드
     */
    @PostConstruct
    public void init() {
        try {
            this.privateKey = loadPrivateKey(jwtProperties.getPrivateKey());
            this.publicKey = loadPublicKey(jwtProperties.getPublicKey());
            log.info("JWT EC 키 페어 로드 완료 (ES256 알고리즘)");
        } catch (Exception e) {
            log.error("JWT 키 페어 로드 실패: {}", e.getMessage(), e);
            throw new RuntimeException("JWT 키 페어 로드 실패", e);
        }
    }

    /**
     * Access Token 생성 (15분)
     * orgId, appTypeId, roleIds를 JWT 클레임에 포함
     *
     * @param user 사용자 정보
     * @return JWT Access Token
     */
    public String generateAccessToken(UserMemberDTO user) {
        // 기본 필드 검증
        if (user == null || user.getEmail() == null || user.getUserId() == null) {
            throw new IllegalArgumentException("사용자 정보, 이메일, 사용자 ID는 필수입니다");
        }

        // Guest JWT (OAuth 초기 로그인): orgId=null 허용
        // Form Login: orgId, appTypeId, roleIds 필수
        boolean isGuestUser = user.getOrgId() == null;

        if (!isGuestUser) {
            // Form Login 사용자는 필수 필드 검증
            if (user.getAppTypeId() == null) {
                throw new IllegalArgumentException("인증된 사용자는 앱 타입 ID가 필수입니다");
            }
            if (user.getRoleIds() == null) {
                throw new IllegalArgumentException("인증된 사용자는 역할 ID가 필수입니다");
            }
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

        return Jwts.builder()
                .subject(user.getEmail())  // 주체: 이메일
                .claim("userId", user.getUserId())  // 사용자 ID
                .claim("orgId", user.getOrgId())  // 현재 활성화된 조직 ID
                .claim("roleIds", user.getRoleIds())  // 역할 ID 배열 (권한 체크용)
                .claim("appTypeId", user.getAppTypeId())  // 조직 타입 ID
                .claim("name", user.getName())  // 사용자 이름
                .issuer(jwtProperties.getIssuer())  // 발급자
                .issuedAt(now)  // 발급 시간
                .expiration(expiryDate)  // 만료 시간
                .signWith(privateKey, Jwts.SIG.ES256)  // ES256 서명
                .compact();
    }

    /**
     * Refresh Token 생성 (7일)
     * 보안을 위해 최소한의 클레임만 포함
     *
     * @param email 사용자 이메일
     * @return JWT Refresh Token
     */
    public String generateRefreshToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration());

        return Jwts.builder()
                .subject(email)  // 주체: 이메일
                .issuer(jwtProperties.getIssuer())  // 발급자
                .issuedAt(now)  // 발급 시간
                .expiration(expiryDate)  // 만료 시간
                .signWith(privateKey, Jwts.SIG.ES256)  // ES256 서명
                .compact();
    }

    /**
     * 토큰에서 이메일(subject) 추출
     *
     * @param token JWT 토큰
     * @return 이메일
     */
    public String getEmailFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    /**
     * 토큰에서 userId 추출 (Access Token만 해당)
     *
     * @param token JWT 토큰
     * @return 사용자 ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 토큰에서 roleIds 추출 (Access Token만 해당)
     *
     * @param token JWT 토큰
     * @return 역할 ID 목록
     */
    public List<Long> getRoleIdsFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        List<?> roleIdsList = claims.get("roleIds", List.class);

        if (roleIdsList == null) {
            return List.of();
        }

        return roleIdsList.stream()
                .map(obj -> {
                    if (obj instanceof Integer) {
                        return ((Integer) obj).longValue();
                    } else if (obj instanceof Long) {
                        return (Long) obj;
                    }
                    return null;
                })
                .filter(id -> id != null)
                .toList();
    }

    /**
     * 토큰에서 orgId 추출 (Access Token만 해당)
     * ⚠️ Guest JWT는 orgId=null 허용
     *
     * @param token JWT 토큰
     * @return 테넌트 org ID (Guest JWT인 경우 null)
     */
    public Long getOrgIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Object orgIdObj = claims.get("orgId");

        // ⚠️ Guest JWT: orgId가 없으면 null 반환
        if (orgIdObj == null) {
            return null;
        }

        // Integer와 Long 타입 모두 처리
        if (orgIdObj instanceof Integer) {
            return ((Integer) orgIdObj).longValue();
        } else if (orgIdObj instanceof Long) {
            return (Long) orgIdObj;
        } else {
            throw new IllegalStateException("orgId claim has invalid type");
        }
    }

    /**
     * 토큰에서 appTypeId 추출 (Access Token만 해당)
     * ⚠️ Guest JWT는 appTypeId=null 허용
     *
     * @param token JWT 토큰
     * @return 조직 타입 ID (Guest JWT인 경우 null)
     */
    public Long getAppTypeIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Object appTypeIdObj = claims.get("appTypeId");

        // ⚠️ Guest JWT: appTypeId가 없으면 null 반환
        if (appTypeIdObj == null) {
            return null;
        }

        // Integer와 Long 타입 모두 처리
        if (appTypeIdObj instanceof Integer) {
            return ((Integer) appTypeIdObj).longValue();
        } else if (appTypeIdObj instanceof Long) {
            return (Long) appTypeIdObj;
        } else {
            throw new IllegalStateException("appTypeId claim has invalid type");
        }
    }

    /**
     * 토큰에서 name 추출 (Access Token만 해당)
     * ⚠️ Guest JWT는 name=null 허용
     *
     * @param token JWT 토큰
     * @return 사용자 이름 (Guest JWT인 경우 null 가능)
     */
    public String getNameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("name", String.class);
    }

    /**
     * 토큰 검증
     *
     * @param token JWT 토큰
     * @return 유효 여부
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(publicKey)  // 공개키로 서명 검증
                    .requireIssuer(jwtProperties.getIssuer())  // 발급자 검증
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT 토큰 만료: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("지원하지 않는 JWT 토큰: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("잘못된 형식의 JWT 토큰: {}", e.getMessage());
        } catch (SignatureException e) {
            log.error("JWT 서명 검증 실패: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT 토큰이 비어있음: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 토큰 만료 시간 조회
     *
     * @param token JWT 토큰
     * @return 만료 시간
     */
    public Date getExpirationFromToken(String token) {
        return getClaimsFromToken(token).getExpiration();
    }

    /**
     * 토큰에서 Claims 추출 (내부 메서드)
     *
     * @param token JWT 토큰
     * @return Claims
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * PEM 파일에서 EC Private Key 로드
     *
     * @param keyPath 키 파일 경로
     * @return PrivateKey
     */
    private PrivateKey loadPrivateKey(String keyPath) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        Resource resource = resourceLoader.getResource(keyPath);
        String keyContent = Files.readString(resource.getFile().toPath());

        // PEM 헤더/푸터 제거
        String privateKeyPEM = keyContent
                .replace("-----BEGIN EC PRIVATE KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END EC PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(privateKeyPEM);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * PEM 파일에서 EC Public Key 로드
     *
     * @param keyPath 키 파일 경로
     * @return PublicKey
     */
    private PublicKey loadPublicKey(String keyPath) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        Resource resource = resourceLoader.getResource(keyPath);
        String keyContent = Files.readString(resource.getFile().toPath());

        // PEM 헤더/푸터 제거
        String publicKeyPEM = keyContent
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(publicKeyPEM);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        return keyFactory.generatePublic(keySpec);
    }
}
