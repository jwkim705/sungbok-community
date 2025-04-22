package com.sungbok.community.dto;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;
import org.jooq.generated.tables.pojos.Members;
import org.jooq.generated.tables.pojos.Users;

@Getter
public class UserMemberDTO {

  private final Long userId;

  private final String email;

  private final String name;

  private final String password;

  private final LocalDate birthdate;

  private final String gender;

  private final String address;

  private final String phoneNumber;

  private final String picture;

  private final Integer registeredByUserId;

  @Builder
  public UserMemberDTO(Users user, Members member) {
    this.userId = user.getId();
    this.email = user.getEmail();
    this.name = member.getName();
    this.password = user.getPassword();
    this.birthdate = member.getBirthdate();
    this.gender = member.getGender();
    this.address = member.getAddress();
    this.phoneNumber = member.getPhoneNumber();
    this.picture = member.getPicture();
    this.registeredByUserId = getRegisteredByUserId();
  }

  public static UserMemberDTO of(Users user, Members member) {
    return UserMemberDTO.builder().user(user).member(member).build();
  }

}
