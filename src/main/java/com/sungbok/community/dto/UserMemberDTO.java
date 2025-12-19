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

  /**
   * 전체 생성자 (jOOQ fetchInto용 + Builder 패턴)
   */
  @Builder(toBuilder = true)
  public UserMemberDTO(
          Long orgId,
          Long userId,
          String email,
          String name,
          String password,
          LocalDate birthdate,
          String gender,
          String address,
          String phoneNumber,
          String picture,
          Long registeredByUserId,
          Long appTypeId,
          List<Long> roleIds
  ) {
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
    this.roleIds = roleIds != null ? roleIds : List.of();  // null이면 빈 리스트
    this.appTypeId = appTypeId;
  }

  /**
   * Users + Memberships POJO로 DTO 생성 (편의 메소드)
   *
   * @param user Users POJO
   * @param membership Memberships POJO
   * @param roleIds 역할 ID 목록
   * @param appTypeId 앱 타입 ID
   * @return UserMemberDTO
   */
  public static UserMemberDTO of(
          Users user,
          Memberships membership,
          List<Long> roleIds,
          Long appTypeId
  ) {
    return UserMemberDTO.builder()
            .orgId(membership.getOrgId())
            .userId(user.getId())
            .email(user.getEmail())
            .name(membership.getName())
            .password(user.getPassword())
            .birthdate(membership.getBirthdate())
            .gender(membership.getGender())
            .address(membership.getAddress())
            .phoneNumber(membership.getPhoneNumber())
            .picture(membership.getPicture())
            .registeredByUserId(membership.getRegisteredByUserId())
            .appTypeId(appTypeId)
            .roleIds(roleIds)
            .build();
  }
}
