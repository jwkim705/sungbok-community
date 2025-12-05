package com.sungbok.community.dto;

import com.sungbok.community.enums.UserRole;
import lombok.Builder;
import lombok.Getter;
import org.jooq.generated.tables.pojos.Members;
import org.jooq.generated.tables.pojos.Users;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

@Getter
public class UserMemberDTO implements Serializable {

  @Serial
  private static final long serialVersionUID = 5146495955594586796L;

  private final Long appId;

  private final Long userId;

  private final String email;

  private final String name;

  private final String password;

  private final @Nullable LocalDate birthdate;

  private final @Nullable String gender;

  private final @Nullable String address;

  private final @Nullable String phoneNumber;

  private final @Nullable String picture;

  private final @Nullable Long registeredByUserId;

  private final UserRole role;

  @Builder
  public UserMemberDTO(Users user, Members member, UserRole role) {
    this.appId = user.getAppId();
    this.userId = user.getId();
    this.email = user.getEmail();
    this.password = user.getPassword();
    this.name = member.getName();
    this.birthdate = member.getBirthdate();
    this.gender = member.getGender();
    this.address = member.getAddress();
    this.phoneNumber = member.getPhoneNumber();
    this.picture = member.getPicture();
    this.role = role;
    this.registeredByUserId = member.getRegisteredByUserId();
  }

  public UserMemberDTO(
          Long appId, Long userId, String email, String name, String password, LocalDate birthdate,
          String gender, String address, String phoneNumber, String picture,
          Long registeredByUserId, UserRole role) {
    this.appId = appId;
    this.userId = userId;
    this.email = email;
    this.name = name;
    this.password = password;
    this.birthdate = birthdate;
    this.gender = gender;
    this.address = address;
    this.phoneNumber = phoneNumber;
    this.picture = picture;
    this.registeredByUserId = registeredByUserId;
    this.role = role;
  }

  public static UserMemberDTO of(Users user, Members member, UserRole role) {
    return UserMemberDTO.builder()
            .user(user)
            .member(member)
            .role(role)
            .build();
  }

}
