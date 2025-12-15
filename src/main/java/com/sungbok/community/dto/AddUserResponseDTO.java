package com.sungbok.community.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jooq.generated.tables.pojos.Memberships;
import org.jooq.generated.tables.pojos.Users;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Getter
@Accessors(chain = true)
public class AddUserResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1628552754217111345L;

    private final Long userId;

    private final String name;

    private final String email;

    private final LocalDate birthday;

    private final String gender;

    private final String address;

    private final String phoneNumber;

    private final String picture;

    private final String nickname;

    private final List<Long> roleIds;


    @Builder
    public AddUserResponseDTO(Users user, Memberships membership, List<Long> roleIds) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.name = membership.getName();
        this.birthday = membership.getBirthdate();
        this.gender = membership.getGender();
        this.address = membership.getAddress();
        this.phoneNumber = membership.getPhoneNumber();
        this.picture = membership.getPicture();
        this.nickname = membership.getNickname();
        this.roleIds = roleIds;
    }

    public static AddUserResponseDTO of(Users user, Memberships membership, List<Long> roleIds) {
        return AddUserResponseDTO
                .builder()
                .user(user)
                .membership(membership)
                .roleIds(roleIds)
                .build();
    }
}
