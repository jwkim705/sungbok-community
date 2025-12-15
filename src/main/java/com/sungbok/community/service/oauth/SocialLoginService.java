package com.sungbok.community.service.oauth;

import com.sungbok.community.dto.SocialUserInfo;
import com.sungbok.community.enums.SocialType;

public interface SocialLoginService {
    SocialType getServiceName();
    String getSocialAccessToken(String code, String codeVerifier);
    SocialUserInfo getSocialUserInfo(String accessToken);
}
