package com.sungbok.community.enums;

import com.sungbok.community.common.exception.DataNotFoundException;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum UserRole {
    GUEST("GUEST"),
    USER("USER"),
    TEACHER("TEACHER"),
    LEADER("LEADER"),
    DIRECTOR("DIRECTOR"),
    PASTOR("PASTOR"),
    ADMIN("ADMIN");

    private final String code;

    UserRole(String code) {
        this.code = code;
    }

    public static UserRole findByCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException(String.format("No matching constant for [%s]", code)));
    }

}

