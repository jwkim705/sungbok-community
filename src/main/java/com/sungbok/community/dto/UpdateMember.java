package com.sungbok.community.dto;

import com.sungbok.community.enums.UserRole;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateMember {

    private Long id;

    private String picture;

    private UserRole role;

    @Builder
    public UpdateMember(Long id, String picture, UserRole role) {
        this.id = id;
        this.picture = picture;
        this.role = role;
    }

}
