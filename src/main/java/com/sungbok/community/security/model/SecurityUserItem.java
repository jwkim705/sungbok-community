package com.sungbok.community.security.model;

import com.rsupport.shuttlecock.entity.User;
import com.rsupport.shuttlecock.enums.UserRole;
import java.io.Serial;
import java.io.Serializable;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
  public SecurityUserItem(User user) {
    this.userId = user.getId();
    this.role = user.getRole();
    this.name = user.getName();
    this.email = user.getEmail();
    this.picture = user.getPicture();
  }

  public static SecurityUserItem of(User user) {
    return SecurityUserItem.builder().user(user).build();
  }
}
