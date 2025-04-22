package com.sungbok.community.security.model;

import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.enums.UserRole;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Getter
@NoArgsConstructor
public class SecurityUserItem implements Serializable {

  @Serial
  private static final long serialVersionUID = -6627142170234780381L;

  private Long userId;

  private UserRole role;

  private String name;

  private String email;

  private String picture;

  @Builder
  public SecurityUserItem(UserMemberDTO user, UserRole role, String picture) {
    this.userId = user.getUserId();
    this.name = user.getName();
    this.role = role;
    this.email = user.getEmail();
    this.picture = picture;
  }

  public static SecurityUserItem of(UserMemberDTO user,  UserRole role, String picture) {
    return SecurityUserItem.builder().user(user).role(role).picture(picture).build();
  }
}
