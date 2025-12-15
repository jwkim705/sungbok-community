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
public class UpdateMember implements Serializable {

    @Serial
    private static final long serialVersionUID = -3338573989333288366L;

    private Long id;

    private String picture;

    private String name;

    @Builder
    public UpdateMember(Long id, String picture, String name) {
        this.id = id;
        this.picture = picture;
        this.name = name;
    }

}
