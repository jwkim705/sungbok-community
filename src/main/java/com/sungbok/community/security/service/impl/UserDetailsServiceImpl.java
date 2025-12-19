package com.sungbok.community.security.service.impl;

import com.sungbok.community.dto.UserMemberDTO;
import com.sungbok.community.repository.UserRepository;
import com.sungbok.community.security.model.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.jooq.generated.tables.pojos.Users;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public @NonNull UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
    // Users 테이블만 조회 (비밀번호 검증용)
    Users user = userRepository.fetchByEmail(username)
        .orElseThrow(() -> new UsernameNotFoundException("회원을 찾을 수 없습니다."));

    // 임시 DTO 생성 (비밀번호만 포함, orgId/roleIds 없음)
    UserMemberDTO tempDto = UserMemberDTO.builder()
        .userId(user.getId())
        .email(user.getEmail())
        .password(user.getPassword())
        .roleIds(List.of())
        .build();

    return new PrincipalDetails(tempDto);
  }
}
