package com.sungbok.community.dto;

import com.sungbok.community.enums.SocialType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialUserInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = 8234567890123456795L;
    private SocialType socialType;
    private String providerId;
    private String email;
    private String name;
    private String picture;
}
