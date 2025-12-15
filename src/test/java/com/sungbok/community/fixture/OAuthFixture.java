package com.sungbok.community.fixture;

import com.sungbok.community.dto.SocialUserInfo;
import com.sungbok.community.enums.SocialType;
import com.sungbok.community.service.oauth.SocialLoginService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

/**
 * OAuth Mock 데이터 생성 Fixture
 * OAuth2IntegrationTest의 중복 제거
 *
 * 사용 예시:
 * <pre>
 * OAuthFixture fixture = OAuthFixture.builder()
 *     .socialType(SocialType.GOOGLE)
 *     .email("oauth@example.com")
 *     .build();
 *
 * SocialUserInfo userInfo = fixture.buildSocialUserInfo();
 * fixture.setupOAuthServiceMock(googleLoginService, "mock-token", "valid-code");
 * </pre>
 */
public class OAuthFixture {

    private SocialType socialType = SocialType.GOOGLE;
    private String email = "oauth@example.com";
    private String name = "OAuth User";
    private String picture = "https://example.com/photo.jpg";

    /**
     * 빌더 시작
     *
     * @return OAuthFixture 인스턴스
     */
    public static OAuthFixture builder() {
        return new OAuthFixture();
    }

    /**
     * 소셜 타입 설정
     *
     * @param socialType 소셜 타입 (GOOGLE, KAKAO, NAVER)
     * @return this
     */
    public OAuthFixture socialType(SocialType socialType) {
        this.socialType = socialType;
        return this;
    }

    /**
     * 이메일 설정
     *
     * @param email 이메일
     * @return this
     */
    public OAuthFixture email(String email) {
        this.email = email;
        return this;
    }

    /**
     * 이름 설정
     *
     * @param name 이름
     * @return this
     */
    public OAuthFixture name(String name) {
        this.name = name;
        return this;
    }

    /**
     * 프로필 사진 URL 설정
     *
     * @param picture 프로필 사진 URL
     * @return this
     */
    public OAuthFixture picture(String picture) {
        this.picture = picture;
        return this;
    }

    /**
     * SocialUserInfo Mock 데이터 생성
     *
     * @return SocialUserInfo
     */
    public SocialUserInfo buildSocialUserInfo() {
        return SocialUserInfo.builder()
                .socialType(socialType)
                .providerId(socialType.name().toLowerCase() + "-" + UUID.randomUUID())
                .email(email)
                .name(name)
                .picture(picture)
                .build();
    }

    /**
     * OAuth Service Mock 설정 (헬퍼 메서드)
     * 중복 코드 제거를 위한 편의 메서드
     *
     * @param loginService    OAuth 로그인 서비스 (Mock)
     * @param mockAccessToken Mock Access Token
     * @param mockCode        Mock Authorization Code
     */
    public void setupOAuthServiceMock(
            SocialLoginService loginService,
            String mockAccessToken,
            String mockCode
    ) {
        // getSocialAccessToken() Mock
        // KAKAO, NAVER는 codeVerifier가 null이므로 nullable() 사용
        when(loginService.getSocialAccessToken(eq(mockCode), nullable(String.class)))
                .thenReturn(mockAccessToken);

        // GOOGLE의 경우 codeVerifier가 필요하므로 anyString() 오버로드도 추가
        when(loginService.getSocialAccessToken(eq(mockCode), anyString()))
                .thenReturn(mockAccessToken);

        // getSocialUserInfo() Mock
        when(loginService.getSocialUserInfo(eq(mockAccessToken)))
                .thenReturn(buildSocialUserInfo());
    }

    /**
     * Fixture 빌더 완료 (메서드 체이닝 종료)
     *
     * @return OAuthFixture 인스턴스
     */
    public OAuthFixture build() {
        return this;
    }
}
