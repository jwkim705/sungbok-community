package com.sungbok.community.config.converter;

import com.sungbok.community.enums.UserRole;
import org.jooq.impl.EnumConverter;

import java.io.Serial;

public class UserRoleConverter extends EnumConverter<String, UserRole> {

    @Serial
    private static final long serialVersionUID = 6942886590520348061L;

    public UserRoleConverter() {
        super(String.class, UserRole.class, UserRole::getCode);
    }
}
