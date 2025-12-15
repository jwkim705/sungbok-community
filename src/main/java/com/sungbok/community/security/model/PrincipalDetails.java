package com.sungbok.community.security.model;

import com.sungbok.community.dto.UserMemberDTO;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
public class PrincipalDetails implements UserDetails {

  @Serial
  private static final long serialVersionUID = 1954914298684455486L;

  private final UserMemberDTO user;

  // Form 로그인 (이메일/비밀번호)
  public PrincipalDetails(UserMemberDTO user) {
    this.user = user;
  }

  //권한을 리턴
  public Collection<? extends GrantedAuthority> getAuthorities() {
    Collection<GrantedAuthority> authorities = new ArrayList<>();

    // 다중 역할 지원
    List<Long> roleIds = user.getRoleIds();
    if (roleIds == null || roleIds.isEmpty()) {
      authorities.add(new SimpleGrantedAuthority("ROLE_GUEST"));
    } else {
      for (Long roleId : roleIds) {
        authorities.add(new SimpleGrantedAuthority("ROLE_" + roleId));
      }
    }

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

  /**
   * 권한 체크를 위한 대표 role ID 반환
   * @return 대표 role ID (첫번째 역할, 없으면 null)
   */
  public Long getRoleId() {
    List<Long> roleIds = user.getRoleIds();
    return (roleIds != null && !roleIds.isEmpty()) ? roleIds.getFirst() : null;
  }

  /**
   * 모든 역할 ID 목록 반환
   * @return 역할 ID 목록
   */
  public List<Long> getRoleIds() {
    return user.getRoleIds();
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