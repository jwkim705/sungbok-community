package com.sungbok.community.security.model;

import com.sungbok.community.dto.UserMemberDTO;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@Getter
public class PrincipalDetails implements UserDetails, OAuth2User {

  @Serial
  private static final long serialVersionUID = 1954914298684455486L;

  private final UserMemberDTO user;
  private Map<String, Object> attributes;

  //일반 로그인
  public PrincipalDetails(UserMemberDTO user) {
    this.user = user;
  }

  //OAuth 로그인
  public PrincipalDetails(UserMemberDTO user, Map<String, Object> attributes) {
    this.user = user;
    this.attributes = attributes;
  }

  //권한을 리턴
  public Collection<? extends GrantedAuthority> getAuthorities() {
    Collection<GrantedAuthority> authorities = new ArrayList<>();
    authorities.add(new SimpleGrantedAuthority(user.getRole().getCode()));
    return authorities;
  }

  @Override
  public String getPassword() {
    // password 필드가 null일 수 있으므로 처리 필요
    return user.getPassword() != null ? user.getPassword() : "";
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
    // OAuth2에서는 고유 식별자를 반환하는 것이 일반적이나, 여기서는 사용자 이름을 반환
    // return attributes != null ? (String) attributes.get("sub") : user.getEmail(); // 예시
    return user.getName(); // 현재 DTO의 name 필드 사용
  }

  // --- UserDetails 기본 구현 메서드 ---
  // 계정 만료 여부 (true: 만료 안됨)
  @Override
  public boolean isAccountNonExpired() {
    return true; // 필요시 로직 추가
  }

  // 계정 잠김 여부 (true: 잠기지 않음)
  @Override
  public boolean isAccountNonLocked() {
    return true; // 필요시 로직 추가
  }

  // 자격 증명(비밀번호) 만료 여부 (true: 만료 안됨)
  @Override
  public boolean isCredentialsNonExpired() {
    return true; // 필요시 로직 추가
  }

  // 계정 활성화 여부 (true: 활성화됨)
  @Override
  public boolean isEnabled() {
    // 예: user.getIsDeleted() 와 같은 필드를 사용
    return true; // 필요시 로직 추가
  }
}