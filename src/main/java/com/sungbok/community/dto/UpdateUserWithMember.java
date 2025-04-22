package com.sungbok.community.dto;

import com.sungbok.community.enums.UserRole;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateUserWithMember {

    private Long userId;

    private Long memberId;

    private String email;

    private String password;

    private String name;

    private String picture;

    private UserRole role;

}
