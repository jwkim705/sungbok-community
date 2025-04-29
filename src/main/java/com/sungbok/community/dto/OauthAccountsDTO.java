package com.sungbok.community.dto;

import com.sungbok.community.enums.SocialType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jooq.generated.tables.pojos.OauthAccounts;
import org.jooq.generated.tables.pojos.Users;

@Getter
@NoArgsConstructor
public class OauthAccountsDTO {

    private Long id;
    private Long userId;
    private SocialType socialType;

    @Builder
    public OauthAccountsDTO(OauthAccounts oauthAccounts, Users user){
        this.id = oauthAccounts.getId();
        this.userId = user.getId();
        this.socialType = SocialType.ofCode(oauthAccounts.getSocialType());
    }

    public static OauthAccountsDTO of(OauthAccounts oauthAccounts) {
        return OauthAccountsDTO.builder().oauthAccounts(oauthAccounts).build();
    }

}
