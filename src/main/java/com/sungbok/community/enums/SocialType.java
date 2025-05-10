package com.sungbok.community.enums;

import com.sungbok.community.common.exception.DataNotFoundException;
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

    public static SocialType findByCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException(String.format("No matching constant for [%s]", code)));
    }
}

