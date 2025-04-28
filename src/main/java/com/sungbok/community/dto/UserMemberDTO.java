package com.sungbok.community.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import org.jooq.generated.tables.pojos.Members;
import org.jooq.generated.tables.pojos.Users;

@Getter
public class UserMemberDTO {

  private final Long userId;

  private final String email;

  private final String name;

  private final LocalDate birthdate;

  private final String gender;

  private final String address;

  private final String phoneNumber;

  private final String picture;

  private final String role;

  private final Long registeredByUserId;

  private final List<DepartmentRoleInfo> departmentRoles; // Changed back to List

  @Value
  public static class DepartmentRoleInfo {
      Long departmentId;
      String departmentName;
      Long roleId;
      String roleName;
      LocalDate assignmentDate;
  }

  @Builder
  public UserMemberDTO(Users user, Members member, String role, List<DepartmentRoleInfo> departmentRoles) {
    this.userId = user.getId();
    this.email = user.getEmail();
    this.name = member.getName();
    this.birthdate = member.getBirthdate();
    this.gender = member.getGender();
    this.address = member.getAddress();
    this.phoneNumber = member.getPhoneNumber();
    this.picture = member.getPicture();
    this.role = role;
    this.departmentRoles = departmentRoles;
    this.registeredByUserId = getRegisteredByUserId();
  }

  public static UserMemberDTO of(Users user, Members member) {
    return UserMemberDTO.builder().user(user).member(member).build();
  }

}
