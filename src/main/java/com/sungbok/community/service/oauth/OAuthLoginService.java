package com.sungbok.community.service.oauth;

import com.sungbok.community.dto.SocialUserInfo;
import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.dto.auth.TokenResponse;
import com.sungbok.community.enums.SocialType;
import com.sungbok.community.repository.UserRepository;
import com.sungbok.community.security.jwt.JwtTokenProvider;
import com.sungbok.community.security.jwt.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthLoginService {

    private final List<SocialLoginService> socialLoginServices;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    public TokenResponse login(String providerName, String code, String codeVerifier) {
        SocialType socialType = SocialType.fromString(providerName);

        SocialLoginService socialService = socialLoginServices.stream()
            .filter(service -> service.getServiceName() == socialType)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 소셜 로그인입니다: " + providerName));

        String socialAccessToken = socialService.getSocialAccessToken(code, codeVerifier);
        SocialUserInfo socialUserInfo = socialService.getSocialUserInfo(socialAccessToken);

        UserMemberDTO user = userRepository.findOrCreateOAuthUser(
            socialUserInfo.getSocialType().name().toLowerCase(),
            socialUserInfo.getProviderId(),
            socialUserInfo.getEmail(),
            socialUserInfo.getName(),
            socialUserInfo.getPicture()
        );

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());
        refreshTokenService.saveRefreshToken(user.getEmail(), refreshToken);

        log.info("{} 로그인 성공: userId={}, email={}", socialType, user.getUserId(), user.getEmail());

        return TokenResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(900)
            .build();
    }
}
