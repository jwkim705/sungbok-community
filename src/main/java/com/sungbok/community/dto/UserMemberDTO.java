package com.sungbok.community.dto;

import lombok.Builder;
import lombok.Getter;
import org.jooq.generated.tables.pojos.Memberships;
import org.jooq.generated.tables.pojos.Users;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Getter
public class UserMemberDTO implements Serializable {

  @Serial
  private static final long serialVersionUID = 5146495955594586796L;

  private final Long orgId;
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
  private final List<Long> roleIds;  // 변경: roleId → roleIds
  private final @Nullable Long appTypeId;  // 추가

  @Builder
  public UserMemberDTO(
          Users user,
          Memberships membership,
          List<Long> roleIds,
          Long appTypeId
  ) {
    this.orgId = membership.getOrgId();
    this.userId = user.getId();
    this.email = user.getEmail();
    this.password = user.getPassword();
    this.name = membership.getName();
    this.birthdate = membership.getBirthdate();
    this.gender = membership.getGender();
    this.address = membership.getAddress();
    this.phoneNumber = membership.getPhoneNumber();
    this.picture = membership.getPicture();
    this.roleIds = roleIds;
    this.appTypeId = appTypeId;
    this.registeredByUserId = membership.getRegisteredByUserId();
  }

  // 전체 생성자 (jOOQ fetchInto용)
  public UserMemberDTO(
          Long orgId, Long userId, String email, String name, String password, LocalDate birthdate,
          String gender, String address, String phoneNumber, String picture,
          Long registeredByUserId, Long appTypeId, List<Long> roleIds) {
    this.orgId = orgId;
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
    this.roleIds = roleIds;
    this.appTypeId = appTypeId;
  }

  public static UserMemberDTO of(
          Users user,
          Memberships membership,
          List<Long> roleIds,
          Long appTypeId
  ) {
    return UserMemberDTO.builder()
            .user(user)
            .membership(membership)
            .roleIds(roleIds)
            .appTypeId(appTypeId)
            .build();
  }
}
