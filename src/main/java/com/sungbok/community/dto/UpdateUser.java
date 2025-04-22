package com.sungbok.community.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateUser {

    private Long id;

    private String email;

    private String password;

    @Builder
    public UpdateUser(Long id, String email, String password) {
        this.id = id;
        this.email = email;
        this.password = password;
    }


}
