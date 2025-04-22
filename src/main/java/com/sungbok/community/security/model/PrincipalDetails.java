package com.sungbok.community.security.model;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import lombok.Getter;
import org.jooq.generated.tables.pojos.Users;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

@Getter
public class PrincipalDetails implements UserDetails, OAuth2User {

  @Serial
  private static final long serialVersionUID = 1954914298684455486L;

  private final Users user;
  private Map<String, Object> attributes;
  private final SecurityUserItem securityUserItem;

  //일반 로그인
  public PrincipalDetails(Users user, SecurityUserItem securityUserItem) {
    this.user = user;
    this.securityUserItem = securityUserItem;
  }

  //OAuth 로그인
  public PrincipalDetails(Users user, Map<String, Object> attributes, SecurityUserItem securityUserItem) {
    this.user = user;
    this.attributes = attributes;
    this.securityUserItem = securityUserItem;
  }

  //권한을 리턴
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    Collection<GrantedAuthority> authorities = new ArrayList<>();
    authorities.add(new SimpleGrantedAuthority(securityUserItem.getRole().getRoleName()));
    return authorities;
  }

  @Override
  public String getPassword() {
    return user.getPassword();
  }

  @Override
  public String getUsername() {
    return user.getEmail();
  }

  //OAuth2
  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Override
  public String getName() {
    return user.getName();
  }
}
