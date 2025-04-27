package com.sungbok.community.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jooq.generated.tables.pojos.Members;
import org.jooq.generated.tables.pojos.Roles;
import org.jooq.generated.tables.pojos.Users;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Accessors(chain = true)
public class AddUserResponseDTO {

    private final Long userId;

    private final List<String> roles;

    private final String name;

    private final String email;

    private final LocalDate birthday;

    private final String gender;

    private final String address;

    private final String phoneNumber;

    private final String picture;

    private final String nickname;


    @Builder
    public AddUserResponseDTO(Users user, Members member, List<Roles> role) {
        this.userId = user.getId();
        this.email = user.getEmail();

        this.name = member.getName();
        this.birthday = member.getBirthdate();
        this.gender = member.getGender();
        this.address = member.getAddress();
        this.phoneNumber = member.getPhoneNumber();
        this.picture = member.getPicture();
        this.nickname = member.getNickname();

        this.roles = role.stream().map(Roles::getName).collect(Collectors.toList());
    }

    public static AddUserResponseDTO of(Users user, Members member, List<Roles> role) {
        return AddUserResponseDTO
                .builder()
                .user(user)
                .member(member)
                .role(role)
                .build();
    }
}
