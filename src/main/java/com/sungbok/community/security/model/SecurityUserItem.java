package com.sungbok.community.security.model;

import com.rsupport.shuttlecock.entity.User;
import com.rsupport.shuttlecock.enums.UserRole;
import java.io.Serial;
import java.io.Serializable;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jooq.generated.tables.pojos.Roles;
import org.jooq.generated.tables.pojos.Users;

@Getter
@NoArgsConstructor
public class SecurityUserItem implements Serializable {

  @Serial
  private static final long serialVersionUID = -6627142170234780381L;

  private Long userId;

  private Roles role;

  private String name;

  private String email;

  private String picture;

  @Builder
  public SecurityUserItem(Users user, Roles role, String picture) {
    this.userId = user.getId();
    this.role = role;
    this.name = user.getName();
    this.email = user.getEmail();
    this.picture = picture;
  }

  public static SecurityUserItem of(Users user) {
    return SecurityUserItem.builder().user(user).build();
  }
}
