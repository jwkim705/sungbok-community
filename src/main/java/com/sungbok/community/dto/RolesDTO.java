package com.sungbok.community.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jooq.generated.tables.pojos.Roles;

import java.io.Serial;
import java.io.Serializable;

@Getter
@NoArgsConstructor
public class RolesDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -1538326834072823949L;

    private Long id;
    private String name;
    private String description;

    @Builder
    public RolesDTO(Roles roles) {
        this.id = roles.getId();
        this.name = roles.getName();
        this.description = roles.getDescription();
    }

    public static RolesDTO of(Roles roles) {
        return RolesDTO.builder().roles(roles).build();
    }

}
