package com.sungbok.community.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class UpdateUserWithMember implements Serializable {

    @Serial
    private static final long serialVersionUID = 4091752259300586255L;

    private Long userId;

    private Long memberId;

    private String email;

    private String password;

    private String name;

    private String picture;

}
