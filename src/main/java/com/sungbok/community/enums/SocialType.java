package com.sungbok.community.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum SocialType {
    GOOGLE("GOOGLE"),
    KAKAO("KAKAO"),
    NAVER("NAVER");

    private final String code;

    SocialType(String code) {
        this.code = code;
    }

    public static SocialType fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("지원하지 않는 OAuth 제공자: %s", code)));
    }

    /**
     * 문자열을 SocialType으로 변환 (대소문자 무시)
     * OAuth 2.1 표준 구현용
     *
     * @param provider 공급자 이름 (google, kakao, naver)
     * @return SocialType
     * @throws IllegalArgumentException 지원하지 않는 공급자인 경우
     */
    public static SocialType fromString(String provider) {
        try {
            return SocialType.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다: " + provider);
        }
    }
}

