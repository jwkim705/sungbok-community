package com.sungbok.community.enums;

import com.sungbok.community.common.exception.DataNotFoundException;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum SocialType {
    GOOGLE("GOOGLE","구글"),
    KAKAO("KAKAO", "카카오"),
    NAVER("NAVER", "네이버");

    private final String value;
    private final String roleName;

    SocialType(String value, String roleName) {
        this.value = value;
        this.roleName = roleName;
    }

    public static SocialType ofCode(String roleName) {
        return Arrays.stream(values())
                .filter(v -> v.roleName.equals(roleName))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException(String.format("No matching constant for [%s]", roleName)));
    }
}

