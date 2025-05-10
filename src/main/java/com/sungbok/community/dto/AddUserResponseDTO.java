package com.sungbok.community.dto;

import com.sungbok.community.enums.UserRole;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jooq.generated.tables.pojos.Members;
import org.jooq.generated.tables.pojos.Users;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

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

    private final UserRole role;


    @Builder
    public AddUserResponseDTO(Users user, Members member, UserRole role) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.name = member.getName();
        this.birthday = member.getBirthdate();
        this.gender = member.getGender();
        this.address = member.getAddress();
        this.phoneNumber = member.getPhoneNumber();
        this.picture = member.getPicture();
        this.nickname = member.getNickname();
        this.role = role;
    }

    public static AddUserResponseDTO of(Users user, Members member, UserRole role) {
        return AddUserResponseDTO
                .builder()
                .user(user)
                .member(member)
                .role(role)
                .build();
    }
}
