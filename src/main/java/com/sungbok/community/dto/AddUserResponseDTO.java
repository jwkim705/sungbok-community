package com.sungbok.community.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jooq.generated.tables.pojos.Members;
import org.jooq.generated.tables.pojos.UserDepartmentRoles;
import org.jooq.generated.tables.pojos.Users;

import java.time.LocalDate;
import java.util.List;

@Getter
@Accessors(chain = true)
public class AddUserResponseDTO {

    private final Long userId;

    private final List<UserDepartmentRoles> departmentRoles;

    private final String name;

    private final String email;

    private final LocalDate birthday;

    private final String gender;

    private final String address;

    private final String phoneNumber;

    private final String picture;

    private final String nickname;


    @Builder
    public AddUserResponseDTO(Users user, Members member, List<UserDepartmentRoles> departmentRoles) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.name = member.getName();
        this.birthday = member.getBirthdate();
        this.gender = member.getGender();
        this.address = member.getAddress();
        this.phoneNumber = member.getPhoneNumber();
        this.picture = member.getPicture();
        this.nickname = member.getNickname();
        this.departmentRoles = departmentRoles;
    }

    public static AddUserResponseDTO of(Users user, Members member, List<UserDepartmentRoles> departmentRoles) {
        return AddUserResponseDTO
                .builder()
                .user(user)
                .member(member)
                .departmentRoles(departmentRoles)
                .build();
    }
}
