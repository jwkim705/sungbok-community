package com.sungbok.community.enums;

import com.sungbok.community.common.exception.DataNotFoundException;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum UserRole {
    GUEST("GUEST", 0L, "손님","ROLE_GUEST"),
    USER("USER", 10L, "성도","ROLE_USER"),
    TEACHER("TEACHER", 20L, "교사","ROLE_TEACHER"),
    LEADER("LEADER", 20L, "리더","ROLE_LEADER"),
    DIRECTOR("DIRECTOR", 30L, "부장(마을장)","ROLE_DIRECTOR"),
    PASTOR("PASTOR", 40L, "목사","ROLE_PASTOR"),
    ADMIN("ADMIN", 50L, "관리자","ROLE_ADMIN");

    private final String value;
    private final long orderScore;
    private final String description;
    private final String roleName;

    UserRole(String value, long orderScore, String description, String roleName) {
        this.value = value;
        this.orderScore = orderScore;
        this.description = description;
        this.roleName = roleName;
    }

    public static UserRole ofCode(String roleName) {
        return Arrays.stream(values())
                .filter(v -> v.roleName.equals(roleName))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException(String.format("No matching constant for [%s]", roleName)));
    }

    public static UserRole ofScore(long score) {
        return Arrays.stream(values())
                .filter(v -> v.orderScore == score)
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException(String.format("No matching constant for [%d]", score)));
    }
}

