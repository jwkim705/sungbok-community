package com.sungbok.community.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class UpdateUser implements Serializable {

    @Serial
    private static final long serialVersionUID = -6249320910790345182L;

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
